package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Category;
import com.example.exception.CustomException;
import com.example.mapper.CategoryMapper;
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
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private RedisUtils redisUtils;

    private static final String CATEGORY_CACHE_KEY = "category:all";

    /**
     * 新增
     */
    public void add(Category category) {
        categoryMapper.insert(category);
        redisUtils.delete(CATEGORY_CACHE_KEY);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        categoryMapper.deleteById(id);
        redisUtils.delete(CATEGORY_CACHE_KEY);
    }

    /**
     * 修改
     */
    public void updateById(Category category) {
        categoryMapper.updateById(category);
        redisUtils.delete(CATEGORY_CACHE_KEY);
    }

    /**
     * 根据ID查询
     */
    public Category selectById(Integer id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 查询所有（带缓存）
     */
    public List<Category> selectAll(Category category) {
        Object cachedCategories = redisUtils.get(CATEGORY_CACHE_KEY);
        if (cachedCategories != null) {
            return (List<Category>) cachedCategories;
        }
        List<Category> categories = categoryMapper.selectAll(category);
        if (!categories.isEmpty()) {
            redisUtils.set(CATEGORY_CACHE_KEY, categories, 1, TimeUnit.DAYS);
        }
        return categories;
    }

    /**
     * 分页查询
     */
    public PageInfo<Category> selectPage(Category category, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Category> list = categoryMapper.selectAll(category);
        return PageInfo.of(list);
    }
}