package com.example.common.config;

import com.example.common.enums.ResultCodeEnum;

/**
 * 统一响应结果封装类
 * 所有接口返回的数据都使用此类封装
 */
public class Result {
    /**
     * 状态码：通过代码告诉前端请求是否成功
     */
    private String code;
    
    /**
     * 消息：如果请求失败，告诉前端错误信息
     */
    private String msg;
    
    /**
     * 数据：前端需要的业务数据
     */
    private Object data;

    /**
     * 私有构造方法：禁止外部直接实例化
     */
    private Result() {}

    /**
     * 根据状态码和消息创建 Result
     */
    private static Result of(String code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /**
     * 根据状态码和消息创建 Result（带数据）
     */
    private static Result of(String code, String msg, Object data) {
        Result result = of(code, msg);
        result.setData(data);
        return result;
    }

    /**
     * 成功（无数据）
     */
    public static Result success() {
        return of(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg());
    }

    /**
     * 成功（带数据）
     */
    public static Result success(Object data) {
        return of(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 失败（默认系统错误）
     */
    public static Result error() {
        return of(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMsg());
    }

    /**
     * 失败（自定义消息）
     */
    public static Result error(String msg) {
        return of(ResultCodeEnum.SYSTEM_ERROR.getCode(), msg);
    }

    /**
     * 失败（自定义状态码和消息）
     */
    public static Result error(String code, String msg) {
        return of(code, msg);
    }

    /**
     * 失败（使用枚举）
     */
    public static Result error(ResultCodeEnum resultCodeEnum) {
        return of(resultCodeEnum.getCode(), resultCodeEnum.getMsg());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}