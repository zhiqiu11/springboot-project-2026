package com.example.controller;

import com.example.common.Result;
import com.example.utils.RedisUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/redis")
public class RedisController {

    @Resource
    private RedisUtils redisUtils;

    @PostMapping("/set")
    public Result set(@RequestParam String key, @RequestParam String value) {
        redisUtils.set(key, value, 60, TimeUnit.SECONDS);
        return Result.success("设置成功");
    }

    @GetMapping("/get")
    public Result get(@RequestParam String key) {
        Object value = redisUtils.get(key);
        return Result.success(value);
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam String key) {
        Boolean deleted = redisUtils.delete(key);
        return Result.success(deleted ? "删除成功" : "删除失败");
    }

    @GetMapping("/exists")
    public Result exists(@RequestParam String key) {
        Boolean exists = redisUtils.exists(key);
        return Result.success(exists);
    }
}