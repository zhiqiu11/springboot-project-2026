package com.example.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.GoodsMapper;
import com.example.mapper.OrderDetailMapper;
import com.example.mapper.OrdersMapper;
import com.example.mapper.UserMapper;
import com.example.utils.OrderUtils;
import com.example.utils.RedisUtils;
import com.example.utils.SaUtils;
import com.revinate.guava.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillService implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SeckillService.class);

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private GoodsService goodsService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserMapper userMapper;

    private RateLimiter limiter;

    private static final long timeoutMinutes = 30;

    // 等当前的bean加载完成后会执行这个方法
    @Override
    public void afterPropertiesSet() throws Exception {
        limiter = RateLimiter.create(10);
    }


    /**
     * 秒杀下单
     * 高并发：同一时间  出现大量的请求
     * 00:00 这一刻 数以万计的请求会打过来  高并发的解决方式就是限流
     * <a href="https://zhuanlan.zhihu.com/p/60979444">秒杀下单</a>
     */
    @Transactional
    public void addFlashOrder(Orders orders) {
        // 设置阻塞，限制秒杀请求
        limiter.acquire();
        User loginUser = SaUtils.getLoginUser();  // 获取当前登录的用户

        // 分布式锁：按用户 ID 加锁，保证同一个人不能并发下单
        String lockKey = "lock:seckill:" + loginUser.getId();
        String lockValue = IdUtil.simpleUUID();
        boolean locked = false;
        try {
            // 自旋等待获取锁，最多等 3 秒
            long start = System.currentTimeMillis();
            while (!(locked = redisUtils.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS))) {
                if (System.currentTimeMillis() - start > 3000) {
                    throw new CustomException("系统繁忙，请稍后重试");
                }
                Thread.sleep(50);
            }

            // 从 orders 中取出购物车列表
            List<Cart> cartList = orders.getCartList();
            if (cartList == null || cartList.isEmpty()) {
                throw new CustomException("购物车不能为空");
            }
            // 秒杀只允许一次买一件，取第一个
            Cart cart = cartList.getFirst();

            // 构造 OrderDetail 对象
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setGoodsId(cart.getGoodsId());
            orderDetail.setNum(cart.getNum());
            doSeckill(orderDetail, orders, loginUser);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException("系统繁忙，请稍后重试");
        } finally {
            if (locked) {
                redisUtils.releaseLock(lockKey, lockValue);
            }
        }
    }

    /**
     * 秒杀核心逻辑：校验 + Redis 扣库存 + 创建订单
     */
    private void doSeckill(OrderDetail orderDetail, Orders orders, User loginUser) {
        Integer goodsId = orderDetail.getGoodsId();
        Goods goods = goodsService.selectById(goodsId);
        
        if (ObjectUtil.isNull(goods)) {  // 判断商品是否存在
            throw new CustomException("未找到商品");
        }
        // 秒杀额度校验
        int flashStore = goods.getFlashNum() - orderDetail.getNum();
        if (flashStore < 0) {
            throw new CustomException("秒杀商品已抢完");
        }
        //秒杀商品校验是否开启秒杀功能
        if(!"是".equals(goods.getHasFlash())){
            throw new CustomException("商品不是秒杀商品");
        }
        // 校验秒杀时间
        if (ObjectUtil.isEmpty(goods.getFlashTime())) {
            throw new CustomException("秒杀时间未设置");
        }
        try {
            DateTime endTime = DateUtil.parse(goods.getFlashTime());
            if (DateUtil.date().isAfter(endTime)) {
                throw new CustomException("秒杀已结束");
            }
        } catch (Exception e) {
            throw new CustomException("秒杀时间格式错误");
        }

        // 提前检查数据库库存
        int store = goods.getStore() - orderDetail.getNum();
        if (store < 0) {
            throw new CustomException("商品库存不足");
        }
        
        //防止超卖功能
        String stockKey = OrderUtils.SECKILL_STOCK_PREFIX + goodsId;//秒杀商品库存key
        String orderKey = "seckill:order:" + loginUser.getId() + ":" + goodsId;//秒杀订单key

        try {
            // 如果 Redis 库存不存在，从数据库读取并初始化
            if (!redisUtils.exists(stockKey)) {
                redisUtils.setIfAbsent(stockKey, goods.getFlashNum(), timeoutMinutes, TimeUnit.MINUTES);
            }
            //防止重复秒杀
            Boolean isExist = redisUtils.setIfAbsent(orderKey, "1", timeoutMinutes, TimeUnit.MINUTES);
            if (!isExist) {
                // 重复下单，此时还没扣库存，无需回滚
                throw new CustomException("您已重复下单");
            }

            Long remain = redisUtils.increment(stockKey, -orderDetail.getNum());
            if (remain < 0) {
                // 库存不足，回滚事务
                redisUtils.increment(stockKey, orderDetail.getNum());
                throw new CustomException("秒杀商品已抢完");
            }

            // 更新秒杀额度、库存、销量
            goods.setFlashNum(flashStore);
            goods.setStore(store);
            goods.setSaleCount(goods.getSaleCount() + orderDetail.getNum());  // 销量也要涨
            goodsService.updateById(goods); // 更新商品库存（内部已清除缓存）

            // 订单详情 - 商品信息（秒杀价格需单独设置）
            orderDetail.setGoodsPrice(goods.getFlashPrice());

            // 订单信息（秒杀价计算总额）
            BigDecimal totalPrice = goods.getFlashPrice().multiply(BigDecimal.valueOf(orderDetail.getNum()));
            orders.setTotal(totalPrice);
            orders.setUserId(loginUser.getId());
            orders.setUserName(loginUser.getName());
            OrderUtils.fillOrderBaseInfo(orders);

//            // 余额校验与扣减（放在 insert 之前，利用 @Transactional 保证原子性）
//            User dbUser = userMapper.selectById(loginUser.getId());
//            if (dbUser.getAccount().compareTo(totalPrice) < 0) {
//                // 余额不足，回滚 Redis 操作
//                redisUtils.increment(stockKey, orderDetail.getNum());
//                redisUtils.delete(orderKey);
//                throw new CustomException("用户余额不足，请充值后重试");
//            }
//            dbUser.setAccount(dbUser.getAccount().subtract(totalPrice));
//            userMapper.updateById(dbUser);

            // 插入订单数据到数据库（此时 orders.id 会被 MyBatis 自动回填）
            ordersMapper.insert(orders);
            // 绑定订单ID到订单详情（必须在 insert orders 之后，因为 ID 是数据库自增的）
            OrderUtils.fillOrderDetail(orderDetail, goods, orders.getId());
            // 插入订单详情数据到数据库
            orderDetailMapper.insert(orderDetail);

            // Cache-Aside：写 DB 后清除用户订单列表缓存，下次查询从 DB 重建
            if (loginUser.getId() != null) {
                OrderUtils.clearAllOrderCache(loginUser.getId(), redisUtils);
            }
        } catch (Exception e) {
            // "重复下单"不需要回滚任何 Redis 操作（orderKey 是上一次请求写入的，不能删）
            if (!"您已重复下单".equals(e.getMessage())) {
                redisUtils.increment(stockKey, orderDetail.getNum());
                redisUtils.delete(orderKey);
            }
            throw e;
        }
    }

    @PostConstruct
    public void preheatSeckillStock() {
        // 1. 构造查询参数，只查秒杀商品
        Goods queryParam = new Goods();
        queryParam.setHasFlash("是");

        // 2. 查询所有秒杀商品
        //    这里需要确认你 GoodsMapper 里是否有 selectAll(Goods goods) 这样的方法。
        //    你在 Step 3 已经为 selectAll 的 XML 增加了 hasFlash 条件，所以可以直接用它。
        List<Goods> seckillGoodsList = goodsMapper.selectAll(queryParam);

        if (seckillGoodsList == null || seckillGoodsList.isEmpty()) {
            logger.info("缓存预热：没有需要预热的秒杀商品");
            return;
        }

        // 3. 逐个写入 Redis（setIfAbsent 内部 NX 保证不覆盖已有数据）
        for (Goods goods : seckillGoodsList) {
            String stockKey = OrderUtils.SECKILL_STOCK_PREFIX + goods.getId();
            redisUtils.setIfAbsent(stockKey, goods.getFlashNum(), 24, TimeUnit.HOURS);
        }

        logger.info("秒杀库存预热完成，共加载 {} 个商品", seckillGoodsList.size());
    }
}