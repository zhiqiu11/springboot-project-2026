package com.example.service;

import com.example.entity.User;
import com.example.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    @Resource
    private RedisUtils redisUtils;

    private static final String TOKEN_PREFIX = "token:";
    private static final long TOKEN_EXPIRE_HOURS = 24;

    public String generateToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        redisUtils.set(key, user, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        return token;
    }

    public User validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = TOKEN_PREFIX + token;
        Object userObj = redisUtils.get(key);
        if (userObj != null) {
            redisUtils.expire(key, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            return (User) userObj;
        }
        return null;
    }

    public void invalidateToken(String token) {
        if (token != null && !token.isEmpty()) {
            String key = TOKEN_PREFIX + token;
            redisUtils.delete(key);
        }
    }

    public boolean renewToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String key = TOKEN_PREFIX + token;
        if (redisUtils.exists(key)) {
            redisUtils.expire(key, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            return true;
        }
        return false;
    }
}