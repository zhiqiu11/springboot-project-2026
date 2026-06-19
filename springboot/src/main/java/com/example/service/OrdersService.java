package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.example.common.enums.OrderStatusEnum;
import com.example.common.enums.RoleEnum;
import com.example.utils.OrderUtils;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务层
 * 负责订单的创建、查询、更新、删除等业务逻辑处理
 * 集成Redis缓存机制，提升查询性能并保证数据一致性
 **/
@Service
public class OrdersService {

    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    GoodsMapper goodsMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    OrderDetailMapper orderDetailMapper;
    @Resource
    CartService cartService;
    @Resource
    private RedisUtils redisUtils;

    @Value("${order.timeout-minutes:15}")  // 默认15分钟
    private long timeoutMinutes;

//    private static final String ORDER_CACHE_PREFIX = "order:";

    /**
     * 创建订单（支持购物车批量下单和单个商品下单）
     * 执行流程：校验用户余额 -> 校验商品库存 -> 创建订单 -> 更新库存 -> 生成订单详情 -> 扣减余额
     * @param orders 订单实体，包含用户ID和购物车列表
     */
    @Transactional
    public void add(Orders orders) {
        List<Cart> cartList = orders.getCartList();
        User user = userMapper.selectById(orders.getUserId());
        
        // 缓存商品信息，避免重复查询数据库
        Map<Integer, Goods> goodsMap = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        // ========== 校验阶段（合并遍历，一次完成总价计算和库存校验）==========
        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMapper.selectById(goodsId);
            goodsMap.put(goodsId, goods);
            
            // 1. 累计计算订单总价
            totalPrice = totalPrice.add(goods.getPrice().multiply(BigDecimal.valueOf(cart.getNum())));
            
            // 2. 校验商品库存是否充足（前置校验，避免部分更新后才发现库存不足）
            if(goods.getStore() < cart.getNum()){
                throw new CustomException("商品库存不足");
            }
        }
        
//        // 3. 校验用户余额是否充足（前置校验，避免后续操作后才发现余额不足）
//        if(user.getAccount().compareTo(totalPrice) < 0){
//            throw new CustomException("用户余额不足，请充值后重试");
//        }
        
        // ========== 执行阶段 ==========
        // 4. 生成订单基础信息并插入数据库
        OrderUtils.fillOrderBaseInfo(orders);
        ordersMapper.insert(orders);
        Integer orderId = orders.getId();

        // 5. 遍历购物车，处理每个商品（使用缓存的商品信息）
        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMap.get(goodsId);  // 直接从缓存获取，不再查询数据库
            
            // 更新商品库存和销量
            goods.setStore(goods.getStore() - cart.getNum());
            goods.setSaleCount(goods.getSaleCount() + cart.getNum());
            goodsMapper.updateById(goods);
            
            // 清除商品缓存（数据已变更，确保下次查询获取最新数据）
            redisUtils.delete("goods:" + goodsId);

            // 创建订单详情记录
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setNum(cart.getNum());
            orderDetail.setGoodsPrice(goods.getPrice());
            OrderUtils.fillOrderDetail(orderDetail, goods, orderId);
            orderDetailMapper.insert(orderDetail);
            
            // 删除购物车对应记录（同时删除Redis缓存）
            cartService.deleteByGoodsId(orders.getUserId(), goodsId);
        }
        
//        // 6. 扣减用户余额
//        user.setAccount(user.getAccount().subtract(totalPrice));
//        userMapper.updateById(user);
        
        // 7. 更新订单总价
        orders.setTotal(totalPrice);
        ordersMapper.updateById(orders);
        
        // 8. 清除用户订单缓存（确保数据一致性，用户下次查询获取最新订单列表）
        if (orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 删除订单（带Redis缓存清理）
     * @param id 订单ID
     */
    @Transactional
    public void deleteById(Integer id) {
        // 获取订单信息（用于后续清除用户订单列表缓存）
        Orders orders = ordersMapper.selectById(id);
        
        // 1. 删除订单Redis缓存
        redisUtils.delete(OrderUtils.getOrderCacheKey(id));
        
        // 2. 删除订单详情记录
        orderDetailMapper.deleteByOrderId(id);
        
        // 3. 删除订单主记录
        ordersMapper.deleteById(id);
        
        // 4. 清除用户订单列表缓存（确保数据一致性）
        if (orders != null && orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 更新订单（带Redis缓存清理）
     * 若订单状态变更为取消，则自动执行退款操作
     * @param orders 订单实体
     */
    @Transactional
    public void updateById(Orders orders) {
        // 若订单状态变为已取消或已退款，执行退款逻辑
        if(OrderStatusEnum.CANCEL.getDesc().equals(orders.getStatus())
                || OrderStatusEnum.REFUNDED.getDesc().equals(orders.getStatus())){
            // 执行退款操作
            refundOrder(orders);
        }
        
        // 更新订单记录
        ordersMapper.updateById(orders);
        
        // 清除用户订单列表缓存（确保数据一致性）
        if (orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 根据ID查询订单（带Redis缓存）
     * 查询策略：先查缓存，缓存未命中时查数据库并写入缓存
     * @param id 订单ID
     * @return 订单实体，未找到返回null
     */
    @Transactional
    public Orders selectById(Integer id) {
        String cacheKey = OrderUtils.getOrderCacheKey(id);
        
        // 1. 优先查询Redis缓存
        Object cachedOrder = redisUtils.get(cacheKey);
        if (cachedOrder != null) {
            return (Orders) cachedOrder;
        }
        
        // 2. 缓存未命中，查询数据库
        Orders orders = ordersMapper.selectById(id);
        // 为对应订单填充订单详情
        fillOrderDetailList(orders);

        // 3. 查询结果写入缓存，设置过期时间为30分钟
        if (orders != null) {
            redisUtils.set(cacheKey, orders, 30, TimeUnit.MINUTES);
        }
        
        return orders;
    }

    /**
     * 查询所有订单列表
     * @param orders 查询条件
     * @return 订单列表
     */
    public List<Orders> selectAll(Orders orders) {
        return ordersMapper.selectAll(orders);
    }

    /**
     * 分页查询订单
     * - 用户端：使用 Redis 缓存，提升查询性能
     * - 管理员端：直接查询数据库，保证数据实时性
     * @param orders 查询条件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param role 用户角色（管理员/普通用户）
     * @return 分页结果
     */
    public PageInfo<Orders> selectPage(Orders orders, Integer pageNum, Integer pageSize, String role) {
        // 管理员端直接查询数据库（数据实时性要求高）
        if (RoleEnum.ADMIN.name().equals(role)) {
            PageHelper.startPage(pageNum, pageSize);
            List<Orders> list = ordersMapper.selectAll(orders);
            // 为每个订单填充订单详情
            for (Orders order : list) {
                fillOrderDetailList(order);
            }
            return PageInfo.of(list);
        }
        
        // 用户端使用 Redis 缓存
        String cacheKey = OrderUtils.buildPageCacheKey(orders, pageNum, pageSize);
        
        // 1. 优先查询缓存
        Object cachedPage = redisUtils.get(cacheKey);
        if (cachedPage != null) {
            @SuppressWarnings("unchecked")
            PageInfo<Orders> cachedOrdersPage = (PageInfo<Orders>) cachedPage;
            return cachedOrdersPage;
        }
        
        // 2. 缓存未命中，查询数据库
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = ordersMapper.selectAll(orders);
        // 为每个订单填充订单详情
        for (Orders order : list) {
            fillOrderDetailList(order);
        }
        PageInfo<Orders> page = PageInfo.of(list);
        
        // 3. 写入缓存，设置过期时间为10分钟
        redisUtils.set(cacheKey, page, 10, TimeUnit.MINUTES);
        
        return page;
    }

    /**
     * 取消超时未支付订单
     * 定时任务调用，自动取消超过指定时间（默认15分钟）未支付的订单
     */
    @Transactional
    public void cancelTimeoutOrders() {
        // 1. 查询所有待支付订单
        Orders query = new Orders();
        query.setStatus(OrderStatusEnum.WAIT_PAY.getDesc());
        List<Orders> notPayOrderList = ordersMapper.selectAll(query);

        // 查询到已完成订单，直接返回
        if(notPayOrderList.isEmpty()){
            return;
        }
        
        // 2. 遍历待支付订单，检查是否超时
        for(Orders order : notPayOrderList){
            // 解析订单创建时间，计算订单创建至今的时间差
            Date createTime = DateUtil.parse(order.getTime(), "yyyy-MM-dd HH:mm:ss");
            long leftTime = new Date().getTime() - createTime.getTime();
            
            // 判断是否超过超时时间（默认15分钟）
            if(leftTime > timeoutMinutes * 60 * 1000){
                // 设置订单状态为已取消
                order.setStatus(OrderStatusEnum.CANCEL.getDesc());
                
                // 执行退款操作（恢复库存、返还余额）
                refundOrder(order);
                
                // 更新订单状态
                ordersMapper.updateById(order);
            }
        }
    }


//    /**
//     * 构建分页缓存键（用户端使用）
//     */
//    private String buildPageCacheKey(Orders orders, Integer pageNum, Integer pageSize) {
//        StringBuilder key = new StringBuilder("order:user:");
//
//        // 用户ID作为目录，确保每个用户独立缓存
//        if (orders.getUserId() != null) {
//            key.append(orders.getUserId()).append(":");
//        } else {
//            // 如果没有用户ID，使用默认标识
//            key.append("default:");
//        }
//
//        key.append(pageNum).append(":").append(pageSize);
//
//        // 添加其他查询条件
//        if (orders.getStatus() != null) {
//            key.append(":status_").append(orders.getStatus());
//        }
//        if (orders.getOrderNo() != null) {
//            key.append(":no_").append(orders.getOrderNo());
//        }
//        if (orders.getGoodsName() != null) {
//            key.append(":goods_").append(orders.getGoodsName());
//        }
//
//        return key.toString();
//    }
//
//    /**
//     * 清除用户所有订单相关缓存（只清除当前用户的）
//     */
//    private void clearAllOrderCache(Integer userId) {
//        try {
//            // 清除当前用户的订单列表缓存（以 order:user:{userId}: 开头）
//            List<String> pageKeys = redisUtils.keys("order:user:" + userId + ":*");
//            if (!pageKeys.isEmpty()) {
//                redisUtils.delete(pageKeys);
//            }
//        } catch (Exception e) {
//            // 处理可能的异常
//        }
//    }


    /**
     * 取消订单的退款处理
     * 执行流程：退回用户余额 -> 恢复商品库存和销量 -> 清除商品缓存
     * 特殊处理：秒杀商品需额外恢复秒杀额度和 Redis 库存
     * @param order 需取消的订单
     */
    private void refundOrder(Orders order) {
        // 查数据库里该订单原始状态
        Orders dbOrder = ordersMapper.selectById(order.getId());
        // 1. 退回用户余额，仅当订单状态不为已取消时才退回余额
        if(!OrderStatusEnum.WAIT_PAY.getDesc().equals(dbOrder.getStatus())){
            User user = userMapper.selectById(order.getUserId());
            user.setAccount(user.getAccount().add(order.getTotal()));
            userMapper.updateById(user);
        }

        // 2. 恢复库存和销量
        OrderDetail detailParam = new OrderDetail();
        detailParam.setOrderId(order.getId());
        List<OrderDetail> detailList = orderDetailMapper.selectAll(detailParam);
        
        for (OrderDetail detail : detailList) {
            Goods goods = goodsMapper.selectById(detail.getGoodsId());
            if (goods != null) {
                // 恢复普通库存和销量（所有订单都需要）
                goods.setStore(goods.getStore() + detail.getNum());
                goods.setSaleCount(goods.getSaleCount() - detail.getNum());

                // 如果该商品是秒杀商品，额外恢复秒杀额度及 Redis 库存
                if ("是".equals(goods.getHasFlash())) {
                    // 恢复数据库秒杀库存
                    goods.setFlashNum(goods.getFlashNum() + detail.getNum());

                    // 恢复 Redis 秒杀库存
                    String stockKey = OrderUtils.SECKILL_STOCK_PREFIX + goods.getId();
                    if (redisUtils.exists(stockKey)) {
                        redisUtils.increment(stockKey, detail.getNum());
                    }
                    
                    // 删除防重复下单标记（让用户取消后可以再次秒杀）
                    String orderKey = "seckill:order:" + order.getUserId() + ":" + goods.getId();
                    redisUtils.delete(orderKey);
                }
                
                goodsMapper.updateById(goods);
                redisUtils.delete("goods:" + goods.getId());
            }
        }
    }

    /**
     * 为订单填充订单详情列表
     * @param order 订单对象
     */
    private void fillOrderDetailList(Orders order) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderDetail detailParam = new OrderDetail();
        detailParam.setOrderId(order.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.selectAll(detailParam);
        order.setOrderDetailList(orderDetailList);
    }

}