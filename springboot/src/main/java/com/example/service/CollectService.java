package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Collect;
import com.example.exception.CustomException;
import com.example.mapper.CollectMapper;
import com.example.utils.RedisUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 业务处理
 **/
@Service
public class CollectService {

    @Resource
    private CollectMapper collectMapper;
    @Resource
    private RedisUtils redisUtils;

    private static final String COLLECT_CACHE_PREFIX = "collect:";

    /**
     * 新增
     */
    public void add(Collect collect) {
        //设置收藏时间
        collect.setTime(DateUtil.now());
        collectMapper.insert(collect);
        
        String key = COLLECT_CACHE_PREFIX + collect.getUserId();
        redisUtils.delete(key);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Collect collect = collectMapper.selectById(id);
        if (collect != null) {
            String key = COLLECT_CACHE_PREFIX + collect.getUserId();
            redisUtils.delete(key);
        }
        collectMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(Collect collect) {
        collectMapper.updateById(collect);
    }

    /**
     * 根据ID查询
     */
    public Collect selectById(Integer id) {
        return collectMapper.selectById(id);
    }

    /**
     * 查询所有（强制同步数据库到 Redis）
     */
    public List<Collect> selectAll(Collect collect) {
        if (collect.getUserId() != null) {
            String key = COLLECT_CACHE_PREFIX + collect.getUserId();
            
            // 先从数据库读取最新数据
            List<Collect> list = collectMapper.selectAll(collect);
            
            // 同步到 Redis（覆盖旧缓存）
            if (!list.isEmpty()) {
                redisUtils.set(key, list, 1, TimeUnit.HOURS);
            } else {
                redisUtils.delete(key);  // 数据库为空，删除缓存
            }
            
            return list;
        }
        return collectMapper.selectAll(collect);
    }

    /**
     * 分页查询（使用缓存）
     */
    public PageInfo<Collect> selectPage(Collect collect, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Collect> list = selectAll(collect);  // 调用带缓存的 selectAll
        return PageInfo.of(list);
    }
}