package com.example.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.OrderStatusEnum;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.OrderDetailMapper;
import com.example.mapper.OrdersMapper;
import com.example.mapper.UserMapper;
import com.example.utils.OrderUtils;
import com.example.utils.RedisUtils;
import com.example.utils.SaUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 团购服务（由 UserPayService.pay 调用，订单创建后支付前）
 */
@Service
public class GroupService {

    @Resource
    private GoodsService goodsService;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtils redisUtils;

    /** 拼团 Redis Key 前缀 */
    private static final String GROUP_KEY_PREFIX = "group:order:";
    /** 拼团有效期（秒） */
    private static final long GROUP_TTL_SECONDS = 3600L;

    /**
     * 创建团购订单（下单即存为待支付，团长开团或团员参团均走此方法）
     */
    @Transactional
    public void addGroupOrder(Orders orders) {
        User loginUser = SaUtils.getLoginUser();

        // 分布式锁：按用户 ID 加锁，防止同一用户并发下单
        String lockKey = "lock:group:" + loginUser.getId();
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

            // ========== 1. 从购物车列表取商品信息 ==========
            List<Cart> cartList = orders.getCartList();
            if (cartList == null || cartList.isEmpty()) {
                throw new CustomException("购物车不能为空");
            }
            Cart cart = cartList.getFirst();
            Integer goodsId = cart.getGoodsId();
            int num = cart.getNum();

            // ========== 2. 校验商品是否存在 & 库存 ==========
            Goods goods = goodsService.selectById(goodsId);
            if (ObjectUtil.isNull(goods)) {
                throw new CustomException("未找到商品");
            }
            if (goods.getStore() < num) {
                throw new CustomException("商品库存不足");
            }
            if (!"是".equals(goods.getHasGroup())) throw new CustomException("该商品未开启团购");
            // 校验团购时间
            if (ObjectUtil.isNotEmpty(goods.getGroupTime())) {
                try {
                    cn.hutool.core.date.DateTime endTime = cn.hutool.core.date.DateUtil.parse(goods.getGroupTime());
                    if (cn.hutool.core.date.DateUtil.date().isAfter(endTime)) {
                        throw new CustomException("团购已结束");
                    }
                } catch (Exception e) {
                    throw new CustomException("团购时间格式错误");
                }
            }
            // ========== 3. 从 orderDetailList 中取 groupOrderId（前端传入） ==========
            Integer groupOrderId = null;
            List<OrderDetail> detailList = orders.getOrderDetailList();
            if (detailList != null && !detailList.isEmpty()) {
                groupOrderId = detailList.getFirst().getGroupOrderId();
            }

            // ========== 4. 参团校验 ==========
            Orders groupOrder = null;
            if (ObjectUtil.isNotNull(groupOrderId)) {
                String groupKey = GROUP_KEY_PREFIX + groupOrderId;

                // 4a. 从 Redis 获取拼团信息
                Map<Object, Object> groupInfo = redisUtils.hGetAll(groupKey);
                if (groupInfo.isEmpty()) {
                    throw new CustomException("拼团不存在或已过期");
                }

                // 4b. 校验拼团状态
                String status = (String) groupInfo.get("status");
                if (!"WAITING".equals(status)) {
                    throw new CustomException("拼团已结束");
                }

                // 4c. 校验商品是否匹配
                Integer goodsIdInGroup = Integer.valueOf((String) groupInfo.get("goodsId"));
                if (!goodsId.equals(goodsIdInGroup)) {
                    throw new CustomException("拼团商品不匹配");
                }

                // 4d. 原子递增参团人数
                Long currentNum = redisUtils.hIncrBy(groupKey, "currentNum", 1);
                Integer targetNum = Integer.valueOf((String) groupInfo.get("targetNum"));
                if (currentNum >= targetNum) {
                    // 达到成团人数 → 更新 Redis 状态为 SUCCESS
                    redisUtils.hSet(groupKey, "status", "SUCCESS");
                }

                // 4e. 查数据库获取团长的订单信息（用于关联）
                groupOrder = ordersMapper.selectById(groupOrderId);
                if (ObjectUtil.isNull(groupOrder)) {
                    throw new CustomException("团长订单不存在");
                }
            }

            // ========== 5. 扣库存 + 创建订单 ==========
            goods.setStore(goods.getStore() - num);
            goods.setSaleCount(goods.getSaleCount() + num);
            goodsService.updateById(goods);

            orders.setGoodsName(goods.getName());
            orders.setTotal(goods.getGroupPrice().multiply(BigDecimal.valueOf(num)));
            orders.setUserId(loginUser.getId());
            OrderUtils.fillOrderBaseInfo(orders);  // 统一设置 orderNo + time + status + userName
            ordersMapper.insert(orders);
            // Cache-Aside：写 DB 后清除用户订单列表缓存
            if (loginUser.getId() != null) {
                OrderUtils.clearAllOrderCache(loginUser.getId(), redisUtils);
            }
            Integer orderId = orders.getId();

            // ========== 6. 持久化 OrderDetail ==========
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setNum(num);
            orderDetail.setGoodsPrice(goods.getGroupPrice());
            orderDetail.setGroupOrderId(groupOrderId); // 记录所属拼团
            OrderUtils.fillOrderDetail(orderDetail, goods, orderId);
            orderDetailMapper.insert(orderDetail);

            // ========== 7. 团长开团：在 Redis 创建拼团记录 ==========
            if (ObjectUtil.isNull(groupOrderId)) {
                String newGroupKey = GROUP_KEY_PREFIX + orderId;
                redisUtils.hSet(newGroupKey, "goodsId", String.valueOf(goodsId));
                redisUtils.hSet(newGroupKey, "targetNum", "2");  // 默认2人成团，可根据需求调整
                redisUtils.hIncrBy(newGroupKey, "currentNum", 1); // 团长自己算1人
                redisUtils.hSet(newGroupKey, "status", "WAITING");
                redisUtils.expire(newGroupKey, GROUP_TTL_SECONDS, TimeUnit.SECONDS);
            }
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
     * 支付后加入拼团（由 UserPayService.pay 在扣款成功后调用）
     * 将团购订单状态从"待支付"更新为"拼团中"，并判断是否成团
     * @return true=团购订单已处理， false=非团购订单（需由调用方设置为"待接单"）
     */
    @Transactional
    public boolean payGroupOrder(Integer orderId) {
        // 1. 查订单详情，获取拼团关联信息
        OrderDetail query = new OrderDetail();
        query.setOrderId(orderId);
        List<OrderDetail> detailList = orderDetailMapper.selectAll(query);
        if (detailList == null || detailList.isEmpty()) {
            return false;
        }
        OrderDetail detail = detailList.getFirst();

        // 2. 确定拼团 key：有 groupOrderId 则是参团，否则自己就是团长（用当前订单ID）
        Integer leaderOrderId = detail.getGroupOrderId();
        if (leaderOrderId == null) {
            leaderOrderId = orderId;
        }
        String groupKey = GROUP_KEY_PREFIX + leaderOrderId;

        // 3. 检查 Redis 拼团记录是否存在（非团购订单直接跳过）
        Map<Object, Object> groupInfo = redisUtils.hGetAll(groupKey);
        if (groupInfo.isEmpty()) {
            return false;
        }

        // 4. 更新当前订单状态为"拼团中"
        Orders dbOrder = ordersMapper.selectById(orderId);
        if (dbOrder != null) {
            dbOrder.setStatus(OrderStatusEnum.IN_GROUP.getDesc());
            ordersMapper.updateById(dbOrder);
        }

        // 5. 判断是否成团：若已满员，将所有团员订单改为"待接单"
        Integer currentNum = Integer.valueOf((String) groupInfo.get("currentNum"));
        Integer targetNum = Integer.valueOf((String) groupInfo.get("targetNum"));
        if (currentNum >= targetNum) {
            // 成团 → 更新 Redis 状态
            redisUtils.hSet(groupKey, "status", "SUCCESS");

            // 遍历 OrderDetail，找到所有关联此拼团的订单，全部改为"待接单"
            OrderDetail searchParam = new OrderDetail();
            searchParam.setGroupOrderId(leaderOrderId);
            List<OrderDetail> allDetails = orderDetailMapper.selectAll(searchParam);
            for (OrderDetail od : allDetails) {
                Orders relatedOrder = ordersMapper.selectById(od.getOrderId());
                if (relatedOrder != null && OrderStatusEnum.IN_GROUP.getDesc().equals(relatedOrder.getStatus())) {
                    relatedOrder.setStatus(OrderStatusEnum.WAIT_ACCEPT.getDesc());
                    ordersMapper.updateById(relatedOrder);
                }
            }
            // 团长订单也要改
            Orders leaderOrder = ordersMapper.selectById(leaderOrderId);
            if (leaderOrder != null && OrderStatusEnum.IN_GROUP.getDesc().equals(leaderOrder.getStatus())) {
                leaderOrder.setStatus(OrderStatusEnum.WAIT_ACCEPT.getDesc());
                ordersMapper.updateById(leaderOrder);
            }
        }

        return true; // 处理了团购逻辑
    }


    /**
     * 按商品ID查询所有活跃拼团（status=WAITING）
     * @param goodsId 商品ID
     * @return 活跃拼团列表
     */
    public List<Map<String, Object>> getActiveGroupsByGoodsId(Integer goodsId) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        // 1. 扫描 Redis 中所有拼团 key
        List<String> groupKeys = redisUtils.keys(GROUP_KEY_PREFIX + "*");
        if (groupKeys == null || groupKeys.isEmpty()) {
            return result;
        }

        for (String key : groupKeys) {
            Map<Object, Object> groupInfo = redisUtils.hGetAll(key);
            if (groupInfo.isEmpty()) {
                continue;
            }

            // 2. 过滤：goodsId 匹配 + 状态为 WAITING
            String goodsIdStr = (String) groupInfo.get("goodsId");
            String status = (String) groupInfo.get("status");
            if (!"WAITING".equals(status) || !String.valueOf(goodsId).equals(goodsIdStr)) {
                continue;
            }

            // 3. 提取 leaderOrderId（从 key 后缀取）
            Integer leaderOrderId = Integer.valueOf(key.substring(GROUP_KEY_PREFIX.length()));

            // 4. 获取 TTL
            Long ttl = redisUtils.getExpire(key, TimeUnit.SECONDS);

            // 5. 查询成员列表（团长 + 参团成员）
            java.util.List<Map<String, Object>> members = new java.util.ArrayList<>();
            // 5a. 先加团长（团长的 OrderDetail.groupOrderId 为 null，需单独查）
            Orders leaderOrder = ordersMapper.selectById(leaderOrderId);
            if (leaderOrder != null) {
                User leaderUser = userMapper.selectById(leaderOrder.getUserId());
                Map<String, Object> leader = new java.util.HashMap<>();
                leader.put("userId", leaderOrder.getUserId());
                leader.put("userName", leaderOrder.getUserName());
                leader.put("avatar", leaderUser != null ? leaderUser.getAvatar() : null);
                members.add(leader);
            }
            // 5b. 再查参团成员（groupOrderId = leaderOrderId）
            OrderDetail memberQuery = new OrderDetail();
            memberQuery.setGroupOrderId(leaderOrderId);
            List<OrderDetail> memberDetails = orderDetailMapper.selectAll(memberQuery);
            for (OrderDetail md : memberDetails) {
                Orders memberOrder = ordersMapper.selectById(md.getOrderId());
                if (memberOrder != null) {
                    User memberUser = userMapper.selectById(memberOrder.getUserId());
                    Map<String, Object> member = new java.util.HashMap<>();
                    member.put("userId", memberOrder.getUserId());
                    member.put("userName", memberOrder.getUserName());
                    member.put("avatar", memberUser != null ? memberUser.getAvatar() : null);
                    members.add(member);
                }
            }

            // 6. 组装结果
            Map<String, Object> group = new java.util.HashMap<>();
            group.put("leaderOrderId", leaderOrderId);
            group.put("currentNum", groupInfo.get("currentNum"));
            group.put("targetNum", groupInfo.get("targetNum"));
            group.put("remainSeconds", ttl != null && ttl > 0 ? ttl : 0);
            group.put("members", members);
            result.add(group);
        }

        return result;
    }

    /**
     * 查询订单的拼团信息（用于前端展示拼团进度）
     * @param orderId 订单ID
     * @return 拼团信息 Map，包含 currentNum、targetNum、remainSeconds、status、members
     */
    public Map<String, Object> getGroupInfo(Integer orderId) {
        // 1. 查订单详情，获取拼团关联信息
        OrderDetail query = new OrderDetail();
        query.setOrderId(orderId);
        List<OrderDetail> detailList = orderDetailMapper.selectAll(query);
        if (detailList == null || detailList.isEmpty()) {
            return null;
        }
        OrderDetail detail = detailList.getFirst();

        // 2. 确定拼团 key：有 groupOrderId 则是参团，否则自己就是团长
        Integer leaderOrderId = detail.getGroupOrderId();
        if (leaderOrderId == null) {
            leaderOrderId = orderId;
        }
        String groupKey = GROUP_KEY_PREFIX + leaderOrderId;

        // 3. 从 Redis 获取拼团信息
        Map<Object, Object> groupInfo = redisUtils.hGetAll(groupKey);
        if (groupInfo.isEmpty()) {
            return null; // 拼团不存在或已过期
        }

        // 4. 获取 TTL（剩余秒数）
        Long ttl = redisUtils.getExpire(groupKey, TimeUnit.SECONDS);

        // 5. 查询所有已参团成员的详情（含头像）
        List<Map<String, Object>> members = new java.util.ArrayList<>();
        OrderDetail memberQuery = new OrderDetail();
        memberQuery.setGroupOrderId(leaderOrderId);
        List<OrderDetail> memberDetails = orderDetailMapper.selectAll(memberQuery);
        for (OrderDetail md : memberDetails) {
            Orders memberOrder = ordersMapper.selectById(md.getOrderId());
            if (memberOrder != null) {
                User memberUser = userMapper.selectById(memberOrder.getUserId());
                Map<String, Object> member = new java.util.HashMap<>();
                member.put("userId", memberOrder.getUserId());
                member.put("userName", memberOrder.getUserName());
                member.put("avatar", memberUser != null ? memberUser.getAvatar() : null);
                member.put("orderId", md.getOrderId());
                members.add(member);
            }
        }

        // 6. 组装返回数据
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("leaderOrderId", leaderOrderId);
        result.put("currentNum", groupInfo.get("currentNum"));
        result.put("targetNum", groupInfo.get("targetNum"));
        result.put("status", groupInfo.get("status"));
        result.put("remainSeconds", ttl > 0 ? ttl : 0);
        result.put("members", members);
        
        return result;
    }

    /**
     * 定时任务：取消超时拼团订单（拼团 Redis key 已过期的"拼团中"订单）
     * 执行流程：扫描所有"拼团中"订单 → 检查 Redis 拼团记录是否存在 → 不存在则退款 + 取消订单
     */
    @Transactional
    public void cancelTimeoutGroupOrders() {
        // 1. 查询所有"拼团中"状态的订单
        Orders query = new Orders();
        query.setStatus(OrderStatusEnum.IN_GROUP.getDesc());
        List<Orders> inGroupOrders = ordersMapper.selectAll(query);
        if (inGroupOrders.isEmpty()) {
            return;
        }

        // 2. 遍历每个"拼团中"订单，检查 Redis 拼团记录是否还存在
        for (Orders order : inGroupOrders) {
            // 2a. 查找该订单的拼团 leaderOrderId
            OrderDetail detailQuery = new OrderDetail();
            detailQuery.setOrderId(order.getId());
            List<OrderDetail> details = orderDetailMapper.selectAll(detailQuery);
            if (details == null || details.isEmpty()) {
                continue;
            }
            OrderDetail detail = details.getFirst();

            // 2b. 确定拼团 Redis key：有 groupOrderId 则是参团，否则自己就是团长
            Integer leaderOrderId = detail.getGroupOrderId();
            if (leaderOrderId == null) {
                leaderOrderId = order.getId();
            }
            String groupKey = GROUP_KEY_PREFIX + leaderOrderId;

            // 2c. 检查 Redis 拼团记录是否还存在
            Map<Object, Object> groupInfo = redisUtils.hGetAll(groupKey);
            if (!groupInfo.isEmpty()) {
                continue; // 拼团记录还在，未超时，跳过
            }

            // 2d. 拼团记录已过期 → 执行退款 + 取消订单
            refundGroupOrder(order);
            order.setStatus(OrderStatusEnum.CANCEL.getDesc());
            ordersMapper.updateById(order);

            // 清除相关缓存
            redisUtils.delete(OrderUtils.getOrderCacheKey(order.getId()));
            if (order.getUserId() != null) {
                OrderUtils.clearAllOrderCache(order.getUserId(), redisUtils);
            }
        }
    }

    /**
     * 拼团超时退款：退还用户余额 + 恢复商品库存和销量
     */
    private void refundGroupOrder(Orders order) {
        // 1. 退还用户余额
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            user.setAccount(user.getAccount().add(order.getTotal()));
            userMapper.updateById(user);
        }

        // 2. 恢复库存和销量
        OrderDetail detailQuery = new OrderDetail();
        detailQuery.setOrderId(order.getId());
        List<OrderDetail> details = orderDetailMapper.selectAll(detailQuery);
        for (OrderDetail detail : details) {
            Goods goods = goodsService.selectById(detail.getGoodsId());
            if (goods != null) {
                goods.setStore(goods.getStore() + detail.getNum());
                goods.setSaleCount(goods.getSaleCount() - detail.getNum());
                goodsService.updateById(goods);
                redisUtils.delete("goods:" + goods.getId());
            }
        }
    }

}