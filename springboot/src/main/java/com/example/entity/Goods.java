package com.example.entity;

import java.math.BigDecimal;

public class Goods {

    /**主键ID */
    private Integer id;
    /**名称 */
    private String name;
    /**图片 */
    private String img;
    /**价格 */
    private BigDecimal price;
    /**简介 */
    private String description;
    /**详情 */
    private String content;
    /**库存 */
    private Integer store;
    /**分类ID */
    private Integer categoryId;
    /**分类名称 */
    private String categoryName;
    /**上架状态 */
    private String status;
    /**浏览量 */
    private Integer views;
    /**销量 */
    private Integer saleCount;
    /**创建时间 */
    private String time;
    /**推荐状态 */
    private String recommend;
    // ========== 秒杀/团购新增字段 ==========
    /**是否秒杀（是/否） */
    private String hasFlash;
    /**是否团购（是/否） */
    private String hasGroup;      //
    /**团购价格 */
    private BigDecimal groupPrice;
    /**秒杀剩余名额 */
    private Integer flashNum;
    /**秒杀价格 */
    private BigDecimal flashPrice;
    /**秒杀结束时间 */
    private String flashTime;
    /**团购结束时间 */
    private String groupTime;
    // ========== 非数据库字段（仅前端展示） ==========
    private transient Long maxTime;        // 剩余秒数（用于倒计时）

    public String getRecommend() {
        return recommend;
    }

    public void setRecommend(String recommend) {
        this.recommend = recommend;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStore() {
        return store;
    }

    public void setStore(Integer store) {
        this.store = store;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(Integer saleCount) {
        this.saleCount = saleCount;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public String getHasFlash() {
        return hasFlash;
    }

    public void setHasFlash(String hasFlash) {
        this.hasFlash = hasFlash;
    }

    public String getHasGroup() {
        return hasGroup;
    }

    public void setHasGroup(String hasGroup) {
        this.hasGroup = hasGroup;
    }

    public Integer getFlashNum() {
        return flashNum;
    }

    public void setFlashNum(Integer flashNum) {
        this.flashNum = flashNum;
    }

    public BigDecimal getGroupPrice() {
        return groupPrice;
    }

    public void setGroupPrice(BigDecimal groupPrice) {
        this.groupPrice = groupPrice;
    }

    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public void setFlashPrice(BigDecimal flashPrice) {
        this.flashPrice = flashPrice;
    }

    public String getFlashTime() {
        return flashTime;
    }

    public void setFlashTime(String flashTime) {
        this.flashTime = flashTime;
    }

    public String getGroupTime() {
        return groupTime;
    }

    public void setGroupTime(String groupTime) {
        this.groupTime = groupTime;
    }

    public Long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Long maxTime) {
        this.maxTime = maxTime;
    }
}