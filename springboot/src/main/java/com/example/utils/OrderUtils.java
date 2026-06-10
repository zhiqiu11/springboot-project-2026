package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.entity.Orders;
import com.example.utils.RedisUtils;

import java.util.Date;
import java.util.List;

public class OrderUtils {

    // 订单缓存前缀
    public static final String ORDER_CACHE_PREFIX = "order:";
    // 用户订单缓存前缀
    public static final String USER_ORDER_CACHE_PREFIX = "order:user:";
    // 秒杀库存缓存前缀
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";

    /**
     * 生成订单编号
     */
    public static String generateOrderNo() {
        return DateUtil.format(new Date(), "yyyyMMdd") + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }//预留方法，后续可能需要根据订单状态生成不同的编号规则

    /**
     * 构建分页缓存键
     */
    public static String buildPageCacheKey(Orders orders, Integer pageNum, Integer pageSize) {
        StringBuilder key = new StringBuilder("order:user:");

        if (orders.getUserId() != null) {
            key.append(orders.getUserId()).append(":");
        } else {
            key.append("default:");
        }

        key.append(pageNum).append(":").append(pageSize);

        if (orders.getStatus() != null) {
            key.append(":status_").append(orders.getStatus());
        }
        if (orders.getOrderNo() != null) {
            key.append(":no_").append(orders.getOrderNo());
        }
        if (orders.getGoodsName() != null) {
            key.append(":goods_").append(orders.getGoodsName());
        }

        return key.toString();
    }

    /**
     * 清除用户所有订单缓存
     */
    public static void clearAllOrderCache(Integer userId, RedisUtils redisUtils) {
        if (userId == null || redisUtils == null) {
            return;
        }
        try {
            List<String> pageKeys = redisUtils.keys("order:user:" + userId + ":*");
            if (pageKeys != null && !pageKeys.isEmpty()) {
                redisUtils.delete(pageKeys);
            }
        } catch (Exception e) {
            // 静默处理缓存清除异常
        }
    }

    /**
     * 获取订单详情缓存键
     */
    public static String getOrderCacheKey(Integer orderId) {
        return ORDER_CACHE_PREFIX + orderId;
    }//预留方法，后续可能需要根据订单详情缓存
}