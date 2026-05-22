package com.example.entity;

/**
 * 轮播图表实体类
 * 用于管理首页轮播图信息，包含关联商品及图片资源
 */
public class Carousel {

    /** 主键ID - 唯一标识每条轮播图记录 */
    private Integer id;

    /** 关联商品 - 用于点击轮播图时跳转到对应商品详情 */
    private Integer goodsId;

    /** 商品名称 - 轮播图展示的商品名称 */
    private String goodsName;

    /** 图片地址 - 轮播图的图片资源路径 */
    private String img;

    /**
     * 获取主键ID
     * @return 返回轮播图记录的唯一标识ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置主键ID
     * @param id 要设置的轮播图ID
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取关联商品ID
     * @return 返回关联的商品ID
     */
    public Integer getGoodsId() {
        return goodsId;
    }

    /**
     * 设置关联商品ID
     * @param goodsId 要设置的商品ID
     */
    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    /**
     * 获取商品名称
     * @return 返回轮播图对应的商品名称
     */
    public String getGoodsName() {
        return goodsName;
    }

    /**
     * 设置商品名称
     * @param goodsName 要设置的商品名称
     */
    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    /**
     * 获取图片地址
     * @return 返回轮播图的图片资源路径
     */
    public String getImg() {
        return img;
    }

    /**
     * 设置图片地址
     * @param img 要设置的图片资源路径
     */
    public void setImg(String img) {
        this.img = img;
    }
}
