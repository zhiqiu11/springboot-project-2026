package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Cart;
import com.example.entity.Goods;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务处理
 **/
@Service
public class CartService {

    @Resource
    private CartMapper cartMapper;
    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private RedisUtils redisUtils;

    private static final String CART_CACHE_PREFIX = "cart:";

    /**
     * 新增（带Redis缓存）
     */
    public void add(Cart cart) {
        // 更新数据库
        Cart dbCart = cartMapper.selectByGoodsIdAndUserId(cart.getGoodsId(), cart.getUserId());
        if (ObjectUtil.isNotEmpty(dbCart)) {
            dbCart.setNum(dbCart.getNum() + cart.getNum());
            cartMapper.updateById(dbCart);
            // 更新Redis缓存
            String key = CART_CACHE_PREFIX + cart.getUserId();
            redisUtils.hSet(key, String.valueOf(cart.getGoodsId()), dbCart.getNum());
        } else {
            cartMapper.insert(cart);
            // 更新Redis缓存
            String key = CART_CACHE_PREFIX + cart.getUserId();
            redisUtils.hSet(key, String.valueOf(cart.getGoodsId()), cart.getNum());
        }
    }

    /**
     * 删除（带Redis缓存）
     */
    public void deleteById(Integer id) {
        Cart cart = cartMapper.selectById(id);
        if (cart != null) {
            // 删除Redis缓存
            String key = CART_CACHE_PREFIX + cart.getUserId();
            redisUtils.hDel(key, String.valueOf(cart.getGoodsId()));
        }
        cartMapper.deleteById(id);
    }

    /**
     * 修改（带Redis缓存）
     */
    public void updateById(Cart cart) {
        // 更新Redis缓存
        String key = CART_CACHE_PREFIX + cart.getUserId();
        if (cart.getNum() <= 0) {
            redisUtils.hDel(key, String.valueOf(cart.getGoodsId()));
        } else {
            redisUtils.hSet(key, String.valueOf(cart.getGoodsId()), cart.getNum());
        }
        cartMapper.updateById(cart);
    }

    /**
     * 根据ID查询
     */
    public Cart selectById(Integer id) {
        return cartMapper.selectById(id);
    }

    /**
     * 查询所有（带Redis缓存）
     */
    public List<Cart> selectAll(Cart cart) {
        if (cart.getUserId() != null) {
            return selectByUserId(cart.getUserId());
        }
        return cartMapper.selectAll(cart);
    }

    /**
     * 分页查询（使用缓存）
     */
    public PageInfo<Cart> selectPage(Cart cart, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Cart> list = selectAll(cart);
        return PageInfo.of(list);
    }

    /**
     * 根据用户ID查询（Redis优先）
     */
    public List<Cart> selectByUserId(Integer userId) {
        List<Cart> cartList = new ArrayList<>();
        String key = CART_CACHE_PREFIX + userId;
        
        Map<Object, Object> cartMap = redisUtils.hGetAll(key);
        if (cartMap == null || cartMap.isEmpty()) {
            List<Cart> dbCarts = cartMapper.selectAll(new Cart(userId));
            if (!dbCarts.isEmpty()) {
                for (Cart cart : dbCarts) {
                    // 过滤无效的购物车记录
                    if (cart.getGoodsId() != null) {
                        redisUtils.hSet(key, String.valueOf(cart.getGoodsId()), cart.getNum());
                    }
                }
            }
            // 过滤掉 goodsId 为 null 的记录
            return dbCarts.stream().filter(cart -> cart.getGoodsId() != null).collect(java.util.stream.Collectors.toList());
        }
        
        for (Map.Entry<Object, Object> entry : cartMap.entrySet()) {
            Object keyObj = entry.getKey();
            // 跳过null或无效的key
            if (keyObj == null || "null".equals(keyObj.toString())) {
                redisUtils.hDel(key, String.valueOf(keyObj));
                continue;
            }
            
            Integer goodsId;
            try {
                goodsId = Integer.parseInt(keyObj.toString());
            } catch (NumberFormatException e) {
                // 无效的goodsId，从Redis中删除
                redisUtils.hDel(key, keyObj.toString());
                continue;
            }
            
            Integer num;
            try {
                num = Integer.parseInt(entry.getValue().toString());
            } catch (NumberFormatException e) {
                redisUtils.hDel(key, String.valueOf(goodsId));
                continue;
            }
            
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods != null) {
                Cart cart = new Cart();
                cart.setGoodsId(goodsId);
                cart.setGoodsName(goods.getName());
                cart.setGoodsImg(goods.getImg());
                cart.setGoodsPrice(goods.getPrice());
                cart.setNum(num);
                cart.setUserId(userId);
                cartList.add(cart);
            } else {
                // 商品不存在，从Redis中删除无效的购物车项
                redisUtils.hDel(key, String.valueOf(goodsId));
            }
        }
        return cartList;
    }

    /**
     * 根据用户ID和商品ID删除
     */
    public void deleteByGoodsId(Integer userId, Integer goodsId) {
        String key = CART_CACHE_PREFIX + userId;
        redisUtils.hDel(key, String.valueOf(goodsId));
        cartMapper.deleteByGoodsIdAndUserId(goodsId, userId);
    }

    /**
     * 更新数量
     */
    public void updateNum(Integer userId, Integer goodsId, Integer num) {
        String key = CART_CACHE_PREFIX + userId;
        if (num <= 0) {
            deleteByGoodsId(userId, goodsId);
            return;
        }
        redisUtils.hSet(key, String.valueOf(goodsId), num);
        
        List<Cart> carts = cartMapper.selectListByGoodsIdAndUserId(goodsId, userId);
        if (carts != null && !carts.isEmpty()) {
            // 如果存在多条记录，只更新第一条，删除其他重复记录
            Cart firstCart = carts.get(0);
            firstCart.setNum(num);
            cartMapper.updateById(firstCart);
            for (Cart c : carts) {
                if (c != firstCart) {
                    cartMapper.deleteById(c.getId());
                }
            }
        }
    }

    /**
     * 删除用户所有购物车
     */
    public void deleteByUserId(Integer userId) {
        String key = CART_CACHE_PREFIX + userId;
        redisUtils.delete(key);
        cartMapper.deleteByUserId(userId);
    }
}