package com.example.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.example.common.enums.RoleEnum;
import com.example.entity.User;
import com.example.entity.Admin;
import com.example.mapper.AdminMapper;
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
    public static Integer getLoginAccountId() {
        // 正常流程：从 TokenSession 中获取 userId
        Object userId = StpUtil.getTokenSession().get("userId");
        if (userId != null) {
            return Integer.valueOf(userId.toString());
        }

        // 降级流程：从 loginId 解析（兼容 TokenSession 中无数据的情况）
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            String[] parts = loginId.toString().split(":");  // 解析 "userId:role"
            return Integer.valueOf(parts[0]);  // 返回 userId 部分
        }

        return null;
    }

    /**
     * 获取当前登录的普通用户
     * @return User 对象，未登录或不是普通用户返回null
     */
    // 在 SaUtils 中添加角色校验
    public static User getLoginUser() {
        Integer userId = getLoginAccountId();
        // 必须同时满足：有用户ID AND 角色是普通用户
        if (userId != null && RoleEnum.USER.name().equals(getLoginRole())) {
            UserMapper userMapper = SpringUtils.getBean(UserMapper.class);
            return userMapper.selectById(userId);
        }
        return null;  // 不是普通用户就返回null
    }

    /**
     * 获取当前登录的管理员
     * @return Admin 对象，未登录或不是管理员返回null
     */
    public static Admin getLoginAdmin() {
        Integer adminId = getLoginAccountId();
        // 必须同时满足：有用户ID AND 角色是管理员
        if (adminId != null && RoleEnum.ADMIN.name().equals(getLoginRole())) {
            AdminMapper adminMapper = SpringUtils.getBean(AdminMapper.class);
            return adminMapper.selectById(adminId);
        }
        return null;  // 不是管理员就返回null
    }
    /**
     * 获取当前登录用户角色
     * @return 角色字符串，未登录返回null
     */
    public static String getLoginRole() {
        Object role = StpUtil.getTokenSession().get("role");  // TokenSession 优先
        if (role != null) return role.toString();
        // 降级：从 loginId 解析
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            String[] parts = loginId.toString().split(":");
            if (parts.length > 1) return parts[1];
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
    public static String loginAndGetToken(Integer userId, String role) {
        String loginId = userId + ":" + role;

        StpUtil.login(loginId);
        // 登录成功后把 userId 和 role 存到 TokenSession
        StpUtil.getTokenSession().set("userId", userId);
        StpUtil.getTokenSession().set("role", role);
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
    public static void forceLogout(Integer userId, String role) {
        String loginId = userId + ":" + role;  // 构建完整的登录标识
        StpUtil.kickout(loginId);
    }
}