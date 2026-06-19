package com.example.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 工具类
 * 封装 RedisTemplate 的常用操作，提供简洁的 API 接口
 */
@Component
public class RedisUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存（无过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键列表
     * @return 删除的键数量
     */
    public Long delete(List<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 根据模式匹配获取所有键
     *
     * @param pattern 匹配模式，如 "goods:*"
     * @return 匹配的键列表
     */
    public List<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null) {
            return List.of();
        }
        return keys.stream().collect(Collectors.toList());
    }

    /**
     * 判断键是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置键的过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置 Hash 缓存
     *
     * @param key     缓存键
     * @param hashKey Hash 字段
     * @param value   字段值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取 Hash 字段值
     *
     * @param key     缓存键
     * @param hashKey Hash 字段
     * @return 字段值，不存在返回 null
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 删除 Hash 字段
     *
     * @param key      缓存键
     * @param hashKeys 要删除的字段列表
     */
    public void hDel(String key, String... hashKeys) {
        redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    /**
     * 获取 Hash 所有字段和值
     *
     * @param key 缓存键
     * @return 所有字段和值的 Map
     */
    public java.util.Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 原子性递增/递减操作
     *
     * @param key   缓存键
     * @param delta 增量（正数递增，负数递减）
     * @return 递增/递减后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 分布式锁：设置值（如果键不存在）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间（防止死锁）
     * @param unit    时间单位
     * @return 是否设置成功（true=获取锁成功，false=锁已存在）
     */
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }
}