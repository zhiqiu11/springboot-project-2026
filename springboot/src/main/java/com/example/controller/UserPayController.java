package com.example.controller;

import com.example.common.config.Result;
import com.example.entity.Orders;
import com.example.service.UserPayService;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userPay")
public class UserPayController {
    @Resource
    private UserPayService userPayService;

    // 支付订单
    @Transactional
    @PostMapping("/pay")
    public Result pay(@RequestBody Orders orders) {
        userPayService.pay(orders);
        return Result.success();
    }
}

