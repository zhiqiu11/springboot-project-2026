package com.example.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> {
                    System.out.println("请求路径: " + cn.dev33.satoken.context.SaHolder.getRequest().getMethod()
                            + " " + cn.dev33.satoken.context.SaHolder.getRequest().getRequestPath());
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",           // 登录
                        "/register",        // 注册
                        "/files/**",        // 文件上传下载
                        "/goods/**",         // 商品接口（测试用）
                        "/error",            // 错误页面
                        "/connections"       // IDEA Dashboard 探测
                );//除了登录接口和注册接口，其他接口都需要登录校验，即登录后才能访问！
    }
}