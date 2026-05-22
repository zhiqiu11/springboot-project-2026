package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Goods;
import com.example.exception.CustomException;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 业务处理
 **/
@Service
public class GoodsService {

    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    CartMapper cartMapper;
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
    }

    /**
     * 修改
     */
    public void updateById(Goods goods) {
        goodsMapper.updateById(goods);
        // 下架删除购物车中的商品
        if (("下架").equals(goods.getStatus())) {
            cartMapper.deleteByGoodsId(goods.getId());
        }
    }

    /**
     * 根据ID查询
     */
    public Goods selectById(Integer id) {
        return goodsMapper.selectById(id);
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
        return PageInfo.of(list);
    }
}