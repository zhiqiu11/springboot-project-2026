package com.example.entity;

import java.math.BigDecimal;
import java.util.function.BiFunction;

public class Cart {

    public Cart() {}

    public Cart(Integer userId) {
        this.userId = userId;
    }

    /**主键ID */
    private Integer id;
    /**商品ID */
    private Integer goodsId;
    /**商品名称 */
    private String goodsName;
    /**商品图片 */
    private String goodsImg;

    /**商品价格 */
    private BigDecimal goodsPrice;
    /**数量 */
    private Integer num;
    /**用户ID */
    private Integer userId;

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

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

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

}