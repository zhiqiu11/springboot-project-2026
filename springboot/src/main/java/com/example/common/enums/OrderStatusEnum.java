package com.example.common.enums;

/**
 * 订单状态枚举类
 * 定义订单生命周期中的所有状态，用于统一管理订单状态常量
 */
public enum OrderStatusEnum {
    
    /**
     * 待接单：订单已创建，等待商家接单
     */
    NOT_PAY("待接单"),
    
    /**
     * 已取消：订单已取消（用户取消或超时自动取消）
     */
    CANCEL("已取消"),
    
    /**
     * 已接单：商家已确认接单
     */
    ACCEPTED("已接单"),
    
    /**
     * 配送中：商品正在配送
     */
    DELIVERING("配送中"),
    
    /**
     * 已完成：订单已完成，交易结束
     */
    COMPLETED("已完成");

    /**
     * 状态描述（中文显示值）
     */
    private final String desc;

    /**
     * 构造方法
     * @param desc 状态描述
     */
    OrderStatusEnum(String desc) {
        this.desc = desc;
    }

    /**
     * 获取状态描述
     * @return 状态的中文描述
     */
    public String getDesc() {
        return desc;
    }
}