package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    private static final String ORDER_CACHE_PREFIX = "order:";

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
        orders.setStatus("待接单");
        orders.setTime(DateUtil.now());
        String orderNo = DateUtil.format(new Date(), "yyyyMMdd") + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        orders.setOrderNo(orderNo);
        ordersMapper.insert(orders);
        Integer orderId = orders.getId();

        for(Cart cart : cartList){
            Integer goodsId = cart.getGoodsId();
            Goods goods = goodsMapper.selectById(goodsId);
            
            //更新商品库存
            goods.setStore(goods.getStore() - cart.getNum());
            //更新商品销售量
            goods.setSaleCount(goods.getSaleCount() + cart.getNum());
            goodsMapper.updateById(goods);

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
        
        // 订单创建成功后写入缓存
        String cacheKey = ORDER_CACHE_PREFIX + orderId;
        redisUtils.set(cacheKey, orders, 30, TimeUnit.MINUTES);
    }

    /**
     * 删除（带Redis缓存）
     */
    @Transactional
    public void deleteById(Integer id) {
        // 删除Redis缓存
        redisUtils.delete(ORDER_CACHE_PREFIX + id);
        // 删除订单详情
        orderDetailMapper.deleteByOrderId(id);
        ordersMapper.deleteById(id);
    }

    /**
     * 修改（带Redis缓存）
     */
    @Transactional
    public void updateById(Orders orders) {
        // 删除Redis缓存
        redisUtils.delete(ORDER_CACHE_PREFIX + orders.getId());
        
        if("已取消".equals(orders.getStatus())){
            //返回用户金额
            Integer userId = orders.getUserId();
            User user = userMapper.selectById(userId);
            //更新用户余额
            user.setAccount(user.getAccount().add(orders.getTotal()));
            userMapper.updateById(user);
            //加商品库存，减商品销售量
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orders.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectAll(orderDetail);
            for(OrderDetail detail : orderDetailList){
                Integer goodsId = detail.getGoodsId();
                Goods goods = goodsMapper.selectById(goodsId);
                if(goods !=null){
                    //更新商品库存
                    goods.setStore(goods.getStore() + detail.getNum());
                    //更新商品销售量
                    goods.setSaleCount(goods.getSaleCount() - detail.getNum());
                    goodsMapper.updateById(goods);
                }
            }
        }
        ordersMapper.updateById(orders);
    }

    /**
     * 根据ID查询（带Redis缓存）
     */
    public Orders selectById(Integer id) {
        String cacheKey = ORDER_CACHE_PREFIX + id;
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
     */
    public PageInfo<Orders> selectPage(Orders orders, Integer pageNum, Integer pageSize) {
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
}