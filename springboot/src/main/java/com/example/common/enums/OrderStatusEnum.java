package com.example.common.enums;

/**
 * 订单状态枚举类
 * 定义订单生命周期中的所有状态，用于统一管理订单状态常量
 */
public enum OrderStatusEnum {

    /**
     * 待支付：订单已创建，等待用户付款
     */
    WAIT_PAY("待支付"),

    /**
     * 待接单：已支付，等待商家接单
     */
    WAIT_ACCEPT("待接单"),

    /**
     * 已接单：商家已确认接单
     */
    ACCEPTED("已接单"),

    /**
     * 配送中：商品正在配送
     */
    DELIVERING("配送中"),

    /**
     * 待收货：商品已送达，等待签收
     */
    NOT_ACCEPT("待收货"),

    /**
     * 已完成：订单已完成，交易结束
     */
    COMPLETED("已完成"),

    /**
     * 已评价：用户已评价（终态）
     */
    COMMENT_DONE("已评价"),

    /**
     * 已取消：订单已取消（未支付取消或超时自动取消）
     */
    CANCEL("已取消"),

    /**
     * 已退款：支付后发生退款，退款完成
     */
    REFUNDED("已退款"),

    /**
     * 拼团中：拼团订单等待成团（成团前不可发货）
     */
    IN_GROUP("拼团中");


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