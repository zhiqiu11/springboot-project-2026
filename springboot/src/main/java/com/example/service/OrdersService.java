package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.example.common.enums.OrderStatusEnum;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 业务处理
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
     * 购物车批量下单和单个商品下单的通用接口
     */
    @Transactional
    public void add(Orders orders) {
        List<Cart> cartList = orders.getCartList();
        User user = userMapper.selectById(orders.getUserId());
        
        //先计算总价并检查余额（避免后续操作后才发现余额不足）
        BigDecimal totalPrice = BigDecimal.ZERO;
        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMapper.selectById(goodsId);
            totalPrice = totalPrice.add(goods.getPrice().multiply(BigDecimal.valueOf(cart.getNum())));
        }
        if(user.getAccount().compareTo(totalPrice) < 0){
            throw new CustomException("用户余额不足，请充值后重试");
        }
        
        //校验商品库存是否足够
        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMapper.selectById(goodsId);
            if(goods.getStore() < cart.getNum()){
                throw new CustomException("商品库存不足");
            }
        }
        
        //所有校验通过后，再执行数据库和Redis操作
        orders.setStatus(OrderStatusEnum.NOT_PAY.getDesc());
        orders.setTime(DateUtil.now());
        String orderNo = DateUtil.format(new Date(), "yyyyMMdd") + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        orders.setOrderNo(orderNo);
        ordersMapper.insert(orders);
        Integer orderId = orders.getId();

        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMapper.selectById(goodsId);
            
            goods.setStore(goods.getStore() - cart.getNum());
            goods.setSaleCount(goods.getSaleCount() + cart.getNum());
            goodsMapper.updateById(goods);
            
            redisUtils.delete("goods:" + goodsId);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setGoodsId(goodsId);
            orderDetail.setGoodsName(goods.getName());
            orderDetail.setNum(cart.getNum());
            orderDetail.setGoodsImg(goods.getImg());
            orderDetail.setGoodsPrice(goods.getPrice());
            orderDetailMapper.insert(orderDetail);
            
            //删除购物车对应记录（同时删除Redis缓存）
            cartService.deleteByGoodsId(orders.getUserId(), goodsId);
        }
        
        //更新用户余额
        user.setAccount(user.getAccount().subtract(totalPrice));
        userMapper.updateById(user);
        
        //更新订单总价
        orders.setTotal(totalPrice);
        ordersMapper.updateById(orders);
        
        // 清除该用户的所有订单相关缓存（确保数据一致性）
        if (orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 删除（带Redis缓存）
     */
    @Transactional
    public void deleteById(Integer id) {
        // 获取订单信息（用于清除用户订单列表缓存）
        Orders orders = ordersMapper.selectById(id);
        
        // 删除Redis缓存
        redisUtils.delete(OrderUtils.getOrderCacheKey(id));
        // 删除订单详情
        orderDetailMapper.deleteByOrderId(id);
        ordersMapper.deleteById(id);
        
        // 清除该用户的所有订单相关缓存（确保数据一致性）
        if (orders != null && orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 修改（带Redis缓存）
     */
    @Transactional
    public void updateById(Orders orders) {
        if(OrderStatusEnum.CANCEL.getDesc().equals(orders.getStatus())){
            //返回用户金额
            refundOrder(orders);
        }
        ordersMapper.updateById(orders);
        
        // 清除该用户的所有订单相关缓存（确保数据一致性）
        if (orders.getUserId() != null) {
            OrderUtils.clearAllOrderCache(orders.getUserId(), redisUtils);
        }
    }

    /**
     * 根据ID查询（带Redis缓存）
     */
    public Orders selectById(Integer id) {
        String cacheKey = OrderUtils.getOrderCacheKey(id);
        // 先查缓存
        Object cachedOrder = redisUtils.get(cacheKey);
        if (cachedOrder != null) {
            return (Orders) cachedOrder;
        }
        // 缓存未命中，查数据库
        Orders orders = ordersMapper.selectById(id);
        if (orders != null) {
            // 写入缓存，设置过期时间为30分钟
            redisUtils.set(cacheKey, orders, 30, TimeUnit.MINUTES);
        }
        return orders;
    }

    /**
     * 查询所有
     */
    public List<Orders> selectAll(Orders orders) {
        return ordersMapper.selectAll(orders);
    }

    /**
     * 分页查询
     * - 用户端：使用 Redis 缓存
     * - 管理员端：直接查询数据库
     */
    public PageInfo<Orders> selectPage(Orders orders, Integer pageNum, Integer pageSize, String role) {
        // 管理员端直接查询数据库，不使用缓存
        if ("管理员".equals(role)) {
            PageHelper.startPage(pageNum, pageSize);
            List<Orders> list = ordersMapper.selectAll(orders);
            for (Orders order : list) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setOrderId(order.getId());
                List<OrderDetail> orderDetailList = orderDetailMapper.selectAll(orderDetail);
                order.setOrderDetailList(orderDetailList);
            }
            return PageInfo.of(list);
        }
        
        // 用户端使用 Redis 缓存
        String cacheKey = OrderUtils.buildPageCacheKey(orders, pageNum, pageSize);
        
        // 先查缓存
        Object cachedPage = redisUtils.get(cacheKey);
        if (cachedPage != null) {
            @SuppressWarnings("unchecked")
            PageInfo<Orders> cachedOrdersPage = (PageInfo<Orders>) cachedPage;
            return cachedOrdersPage;
        }
        
        // 缓存未命中，查数据库
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = ordersMapper.selectAll(orders);
        for (Orders order : list) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectAll(orderDetail);
            order.setOrderDetailList(orderDetailList);
        }
        PageInfo<Orders> page = PageInfo.of(list);
        
        // 写入缓存，设置过期时间为10分钟
        redisUtils.set(cacheKey, page, 10, TimeUnit.MINUTES);
        
        return page;
    }

    @Transactional
    public void cancelTimeoutOrders() {
        //先去查询所有待支付订单
        Orders query = new Orders();//创建目标筛选订单
        query.setStatus(OrderStatusEnum.NOT_PAY.getDesc());//指定状态为待支付订单
        List<Orders> notPayOrderList = ordersMapper.selectAll(query);
        if(notPayOrderList.isEmpty()){return;}
        //再遍历所有待支付订单，调用订单服务的取消订单方法
        for(Orders order : notPayOrderList){
            //循环内部，先获取订单的创建时间，再将其转化为合适的格式，最后和当前时间进行减法运算，判断订单是否超时
            Date createTime = DateUtil.parse(order.getTime(),"yyyy-MM-dd HH:mm:ss");
            long leftTime = new Date().getTime() - createTime.getTime();
            if(leftTime > timeoutMinutes * 60 * 1000){
                //更新订单状态为已取消状态
                //先设置订单状态为已取消状态
                order.setStatus(OrderStatusEnum.CANCEL.getDesc());

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
     * 取消订单的退款处理：退回用户余额、恢复商品库存、清除商品缓存
     * @param order 需取消的订单
     */
    private void refundOrder(Orders order) {
        // 退余额
        User user = userMapper.selectById(order.getUserId());
        user.setAccount(user.getAccount().add(order.getTotal()));
        userMapper.updateById(user);

        // 恢复库存和销量
        OrderDetail detailParam = new OrderDetail();
        detailParam.setOrderId(order.getId());
        List<OrderDetail> detailList = orderDetailMapper.selectAll(detailParam);
        for (OrderDetail detail : detailList) {
            Goods goods = goodsMapper.selectById(detail.getGoodsId());
            if (goods != null) {
                goods.setStore(goods.getStore() + detail.getNum());
                goods.setSaleCount(goods.getSaleCount() - detail.getNum());
                goodsMapper.updateById(goods);
                redisUtils.delete("goods:" + goods.getId());
            }
        }
    }
}