package com.example.common.enums;

/**
 * 统一响应状态码枚举
 * 用于管理所有接口的状态码和错误消息
 */
public enum ResultCodeEnum {

    // ============ 成功状态码 ============
    SUCCESS("200", "请求成功"),

    // ============ 客户端错误（4xx） ============
    BAD_REQUEST("400", "请求参数错误"),
    LOGIN_FAILED("400", "登录失败，用户不存在"),
    PASSWORD_ERROR("400", "密码错误"),
    PASSWORD_NOT_MATCH("400", "两次输入的密码不一致"),
    USER_EXISTS("400", "用户已存在"),
    DATA_NOT_FOUND("400", "数据不存在"),
    
    // ============ 认证错误（401） ============
    UNAUTHORIZED("401", "未授权，请登录"),
    TOKEN_INVALID("401", "Token无效，请重新登录"),
    TOKEN_EXPIRED("401", "Token已过期，请重新登录"),
    NOT_LOGIN("401", "请先登录"),
    
    // ============ 权限错误（403） ============
    FORBIDDEN("403", "暂无权限访问"),
    
    // ============ 服务端错误（5xx） ============
    SYSTEM_ERROR("500", "系统异常，请稍后重试"),
    DATABASE_ERROR("500", "数据库操作异常"),
    SERVICE_UNAVAILABLE("503", "服务暂不可用");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态消息
     */
    private final String msg;

    /**
     * 构造方法
     * @param code 状态码
     * @param msg 状态消息
     */
    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取状态码
     * @return 状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态消息
     * @return 状态消息
     */
    public String getMsg() {
        return msg;
    }
}