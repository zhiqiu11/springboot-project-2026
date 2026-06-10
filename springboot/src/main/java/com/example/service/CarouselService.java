package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Carousel;
import com.example.exception.CustomException;
import com.example.mapper.CarouselMapper;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 业务处理
 **/
@Service
public class CarouselService {

    @Resource
    private CarouselMapper carouselMapper;
    @Resource
    private RedisUtils redisUtils;

    private static final String CAROUSEL_CACHE_KEY = "carousel:all";

    /**
     * 新增
     */
    public void add(Carousel carousel) {
        carouselMapper.insert(carousel);
        redisUtils.delete(CAROUSEL_CACHE_KEY);
        //Cache-Aside（旁路缓存） 模式：修改数据库内容后，缓存中删除对应的键
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        carouselMapper.deleteById(id);
        redisUtils.delete(CAROUSEL_CACHE_KEY);
    }

    /**
     * 修改
     */
    public void updateById(Carousel carousel) {
        carouselMapper.updateById(carousel);
        redisUtils.delete(CAROUSEL_CACHE_KEY);
    }

    /**
     * 根据ID查询
     */
    public Carousel selectById(Integer id) {
        return carouselMapper.selectById(id);
    }

    /**
     * 查询所有（带缓存）
     */
    public List<Carousel> selectAll(Carousel carousel) {
        Object cachedCarousels = redisUtils.get(CAROUSEL_CACHE_KEY);
        if (cachedCarousels != null) {
            return (List<Carousel>) cachedCarousels;
        }
        List<Carousel> carousels = carouselMapper.selectAll(carousel);
        if (!carousels.isEmpty()) {
            redisUtils.set(CAROUSEL_CACHE_KEY, carousels, 1, TimeUnit.DAYS);
        }
        return carousels;
    }

    /**
     * 分页查询
     */
    public PageInfo<Carousel> selectPage(Carousel carousel, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Carousel> list = carouselMapper.selectAll(carousel);
        return PageInfo.of(list);
    }
}