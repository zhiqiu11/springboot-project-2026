package com.example.common;

public class Result {
    private String code;//通过代码告诉前端请求是否成功
    private String msg;//如果请求失败，则告诉前端错误信息
    private Object data;//前端向后台返回的数据

    private Result(Object data) {
        this.data = data;
    }
    //所有接口返回的数据都是同一个类型result。
    public Result() {
    }

    public static Result success() {
        Result result = new Result();
        result.setCode("200");
        result.setMsg("请求成功");
        return result;
    }

    public static Result success(Object data) {
        Result result = success();
        result.setData(data);
        return result;
    }

    public static Result error() {
        Result result = new Result();
        result.setCode("500");
        result.setMsg("请求失败");
        return result;
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.setCode("500");
        result.setMsg(msg);
        return result;
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
