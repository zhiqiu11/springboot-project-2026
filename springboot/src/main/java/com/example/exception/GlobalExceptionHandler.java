package com.example.exception;

import cn.dev33.satoken.exception.SaTokenException;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.example.common.config.Result;
import com.example.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@ControllerAdvice(basePackages = "com.example.controller")
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();

    @ExceptionHandler(SaTokenException.class)
    @ResponseBody//返回json串
    public Result saTokenError(HttpServletRequest request, SaTokenException e) {
        log.error("权限异常：", e);
        return Result.error(ResultCodeEnum.TOKEN_INVALID.getCode(), ResultCodeEnum.TOKEN_INVALID.getMsg());
    }

    //统一异常处理@ExceptionHandler,主要用于Exception
    @ExceptionHandler(Exception.class)
    @ResponseBody//返回json串
    public Result error(HttpServletRequest request, Exception e) {
        log.error("异常信息：", e);
        return Result.error();
    }

    //OrdersService（订单服务）异常处理
    @ExceptionHandler(CustomException.class)
    @ResponseBody//返回json串
    public Result customError(HttpServletRequest request, CustomException e) {
        return Result.error(e.getMsg());
    }

}
