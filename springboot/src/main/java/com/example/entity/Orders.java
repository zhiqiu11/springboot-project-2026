package com.example.entity;

import java.math.BigDecimal;
import java.util.List;

public class Orders {

    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 总价格
     */
    private BigDecimal total;
    /**
     * 用户ID
     */
    private Integer userId;
    /**
     * 用户姓名
     */
    private String userName;
    /**
     * 状态
     */
    private String status;
    /**
     * 下单时间
     */
    private String time;
    /**
     * 配送类型
     */
    private String deliverType;
    /**
     * 配送地址
     */
    private String address;
    /**
     * 配送信息
     */
    private String deliver;
    /**
     * 商品名称
     */
    private String goodsName;
    /**
     * 购物车列表
     */
    private List<Cart> cartList;
    /**
     * 订单详情
     */
    private List<OrderDetail> orderDetailList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDeliverType() {
        return deliverType;
    }

    public void setDeliverType(String deliverType) {
        this.deliverType = deliverType;
    }

    public List<Cart> getCartList() {
        return cartList;
    }

    public void setCartList(List<Cart> cartList) {
        this.cartList = cartList;
    }

    public List<OrderDetail> getOrderDetailList() {
        return orderDetailList;
    }

    public void setOrderDetailList(List<OrderDetail> orderDetailList) {
        this.orderDetailList = orderDetailList;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDeliver() {
        return deliver;
    }

    public void setDeliver(String deliver) {
        this.deliver = deliver;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }
}