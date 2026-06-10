package com.example.system;

import com.example.service.OrdersService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class OrderScheduleTask {
    //创建一个logger对象，用于记录日志
    private static final Logger logger = LoggerFactory.getLogger(OrderScheduleTask.class);

    @Resource
    private OrdersService ordersService;

    @Scheduled(cron = "0 0/5 * * * ?")//cron表达式：0秒0分0时0天0周，即每分钟执行一次任务
    public void cancelTimeoutOrders() {
        logger.info("定时任务：开始扫描超时订单");
        try {
            //调用订单服务的取消超时订单方法
            ordersService.cancelTimeoutOrders();
            logger.info("定时任务：超时订单扫描完成");
        } catch (Exception e) {
            logger.error("定时任务：超时订单扫描失败", e);
        }
    }
}