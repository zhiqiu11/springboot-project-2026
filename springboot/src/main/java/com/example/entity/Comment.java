package com.example.entity;

/**
 * 评论实体类
 */
public class Comment {

    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 评分
     */
    private Double score;
    /**
     * 评价内容
     */
    private String content;
    /**
     * 评价人ID
     */
    private Integer userId;
    /**
     * 评价人姓名
     */
    private String userName;

    /**
     * 评价人头像
     */
    private String userAvatar;
    /**
     * 评价时间
     */
    private String time;
    /**
     * 订单ID
     */
    private Integer orderId;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 商品ID
     */
    private Integer goodsId;
    /**
     * 商品名称
     */
    private String goodsName;
    /**
     * 商品图片
     */
    private String goodsImg;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public String getUserAvatar() {
        return userAvatar;
    }
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
}