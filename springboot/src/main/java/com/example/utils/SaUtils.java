package com.example.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.example.entity.User;
import com.example.mapper.UserMapper;

/**
 * Sa-Token工具类
 * 封装常用的Sa-Token操作，便于统一管理和复用
 */
public class SaUtils {

    /**
     * 获取当前登录用户ID
     * @return 用户ID，未登录返回null
     */
    public static Integer getLoginUserId() {
        Object loginId = StpUtil.getLoginId();
        if (loginId != null) {
            return Integer.valueOf(loginId.toString());
        }
        return null;
    }

    /**
     * 获取当前的登录用户
     * @return loginUser，未登录返回null
     */
    public static User getLoginUser() {
        Integer userId = getLoginUserId();
        if (userId != null) {
            UserMapper userMapper = SpringUtils.getBean(UserMapper.class);
            return userMapper.selectById(userId);
        }
        return null;
    }

    /**
     * 用户登录
     * @param userId 用户ID
     */
    public static void login(Integer userId) {
        StpUtil.login(userId);
    }

    /**
     * 用户登录并返回Token
     * @param userId 用户ID
     * @return Token字符串
     */
    public static String loginAndGetToken(Integer userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    /**
     * 获取当前Token值
     * @return Token字符串
     */
    public static String getToken() {
        return StpUtil.getTokenValue();
    }

    /**
     * 用户登出
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * 检查是否已登录
     * @return 已登录返回true，否则返回false
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 强制登出指定用户
     * @param userId 用户ID
     */
    public static void forceLogout(Integer userId) {
        StpUtil.kickout(userId);
    }
}