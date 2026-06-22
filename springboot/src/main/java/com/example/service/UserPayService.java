package com.example.service;

import com.example.common.enums.OrderStatusEnum;
import com.example.entity.Orders;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.OrdersMapper;
import com.example.mapper.UserMapper;
import com.example.utils.OrderUtils;
import com.example.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPayService {
    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private GroupService groupService;

    // pay(Orders orders) 方法执行：
    // 1. 查询订单，校验状态是否为"待支付"
    // 2. 查询用户，校验余额是否充足
    // 3. 扣减用户余额
    // 4. 更新订单状态为"待接单"
    // 5. 清除相关 Redis 缓存
    @Transactional
    public void pay(Orders orders) {
        // 1. 从数据库查询订单原始状态，校验是否为"待支付"
        Orders dbOrder = ordersMapper.selectById(orders.getId());
        if (dbOrder != null && OrderStatusEnum.WAIT_PAY.getDesc().equals(dbOrder.getStatus())) {
            // 2. 查询用户，校验余额是否充足
            // 余额校验与扣减（放在 insert 之前，利用 @Transactional 保证原子性）
            User user = userMapper.selectById(orders.getUserId());
            //校验用户余额是否充足（前置校验，避免后续操作后才发现余额不足）
            if(user.getAccount().compareTo(dbOrder.getTotal()) < 0){
                throw new CustomException("用户余额不足，请充值后重试");
            }
            // 3. 扣减用户余额
            user.setAccount(user.getAccount().subtract(dbOrder.getTotal()));
            userMapper.updateById(user);
            // 4. 如果是团购订单，先处理拼团逻辑（状态由 payGroupOrder 内部控制）
            boolean isGroupOrder = groupService.payGroupOrder(dbOrder.getId());
            // 5. 非团购订单直接改为"待接单"（团购订单状态已在 payGroupOrder 中设置）
            if (!isGroupOrder) {
                dbOrder.setStatus(OrderStatusEnum.WAIT_ACCEPT.getDesc());
                ordersMapper.updateById(dbOrder);
            }
            // 6. 清除该订单的缓存（否则前端读到的还是旧的"待支付"状态）
            redisUtils.delete(OrderUtils.getOrderCacheKey(dbOrder.getId()));
            // 7. 清除用户订单列表缓存（确保数据一致性）
            if (dbOrder.getUserId() != null) {
                OrderUtils.clearAllOrderCache(dbOrder.getUserId(), redisUtils);
            }
        }
    }
}
