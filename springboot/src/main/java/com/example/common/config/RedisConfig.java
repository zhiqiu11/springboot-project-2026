package com.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 该类用于配置Spring Data Redis的RedisTemplate，
 * 定义了键和值的序列化方式，确保数据在Redis中正确存储和读取。
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate Bean
     * RedisTemplate是Spring Data Redis提供的核心操作类，
     * 用于与Redis进行交互。
     *
     * @param connectionFactory Redis连接工厂，由Spring自动注入
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置Redis连接工厂
        template.setConnectionFactory(connectionFactory);

        // 字符串序列化器：用于序列化Redis的键（Key）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // JSON序列化器：用于序列化Redis的值（Value），支持复杂对象
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置键的序列化器（普通键和哈希键）
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 设置值的序列化器（普通值和哈希值）
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 完成配置初始化
        template.afterPropertiesSet();

        return template;
    }
}