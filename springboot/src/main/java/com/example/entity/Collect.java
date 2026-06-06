package com.example.entity;

import java.math.BigDecimal;

public class Collect {

    public Collect() {}

    public Collect(Integer userId) {
        this.userId = userId;
    }

    /**ID */
    private Integer id;
    /**商品ID */
    private Integer goodsId;
    /**商品名称 */
    private String goodsName;
    /**用户ID */
    private Integer userId;
    /**用户名 */
    private String userName;
    /**收藏时间 */
    private String time;
    /**商品价格 */
    private BigDecimal goodsPrice;
    /**商品图片 */
    private String goodsImg;

    // Getter 和 Setter 方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }
}