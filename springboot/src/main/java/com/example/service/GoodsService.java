package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Goods;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 业务处理
 **/
@Service
public class GoodsService {

    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    CartMapper cartMapper;
    @Resource
    private RedisUtils redisUtils;

    private static final String GOODS_CACHE_PREFIX = "goods:";
    /**
     * 新增
     */
    public void add(Goods goods) {
        goods.setViews(0);
        goods.setSaleCount(0);
        String now = DateUtil.now(); // 获取当前时间
        goods.setTime(now);
        goodsMapper.insert(goods);
    }

    /**
     * 删除
     */
    @Transactional
    public void deleteById(Integer id) {
        // 先删除购物车中的商品
        goodsMapper.deleteById(id);
        // 再删除商品
        cartMapper.deleteByGoodsId(id);
        // 删除缓存
        redisUtils.delete(GOODS_CACHE_PREFIX + id);
    }

    /**
     * 修改
     */
    public void updateById(Goods goods) {
        goodsMapper.updateById(goods);
        // 删除缓存
        redisUtils.delete(GOODS_CACHE_PREFIX + goods.getId());
        // 下架删除购物车中的商品
        if (("下架").equals(goods.getStatus())) {
            cartMapper.deleteByGoodsId(goods.getId());
        }
    }

    /**
     * 根据ID查询（带缓存）
     */
    public Goods selectById(Integer id) {
        String cacheKey = GOODS_CACHE_PREFIX + id;
        // 先查缓存
        Object cachedGoods = redisUtils.get(cacheKey);
        if (cachedGoods != null) {
            return (Goods) cachedGoods;
        }
        // 缓存未命中，查数据库
        Goods goods = goodsMapper.selectById(id);
        if (goods != null) {
            // 计算秒杀剩余时间（与 selectPage 逻辑一致）
            calculateLastTime(List.of(goods));
            // 写入缓存，设置过期时间为1小时（可根据实际情况调整）
            redisUtils.set(cacheKey, goods, 1, TimeUnit.HOURS);
        }
        return goods;
    }

    /**
     * 查询所有
     */
    public List<Goods> selectAll(Goods goods) {
        return goodsMapper.selectAll(goods);
    }

    /**
     * 分页查询
     */
    public PageInfo<Goods> selectPage(Goods goods, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Goods> list = goodsMapper.selectAll(goods);
        calculateLastTime(list);
        return PageInfo.of(list);
    }

    private void calculateLastTime(List<Goods> list) {
        for (Goods dbGoods : list) {
            long maxTime = 0L;
            // 秒杀：计算 flashTime 剩余秒数
            if (("是").equals(dbGoods.getHasFlash()) && ObjectUtil.isNotEmpty(dbGoods.getFlashTime())) {
                long gap = parseRemainSeconds(dbGoods.getFlashTime());
                if (gap > maxTime) maxTime = gap;
            }
            // 团购：计算 groupTime 剩余秒数
            if (("是").equals(dbGoods.getHasGroup()) && ObjectUtil.isNotEmpty(dbGoods.getGroupTime())) {
                long gap = parseRemainSeconds(dbGoods.getGroupTime());
                if (gap > maxTime) maxTime = gap;
            }
            dbGoods.setMaxTime(maxTime);
        }
    }

    private long parseRemainSeconds(String timeStr) {
        try {
            long now = System.currentTimeMillis();
            Date end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timeStr);
            long gap = (end.getTime() - now) / 1000;
            return Math.max(gap, 0);
        } catch (ParseException e) {
            return 0L;
        }
    }

}