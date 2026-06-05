<template>
  <div class="front-container">
    <div style="display: flex;grid-gap: 20px;margin-bottom: 20px">
      <div style="flex: 1;display: flex;align-items: center; grid-gap: 20px">
        <div :class="{'active': null == data.categoryId}"
             @click="loadByCategory(null)"
             style="cursor: pointer; margin-right: 10px;padding-bottom: 5px">全部</div>
        <div :class="{'active': item.id == data.categoryId}"
             @click="loadByCategory(item.id)"
             style="cursor: pointer; margin-right: 10px;padding-bottom: 5px"
             v-for="item in data.categoryList" :key="item.id">{{item.name}}</div>
      </div>
      <div>
        <el-input clearable @clear="load" style="width: 300px; height: 40px" v-model="data.name" placeholder="请输入商品名称搜索"></el-input>
        <el-button type="primary" @click="load" style="height: 40px;">搜索</el-button>
      </div>
    </div>

    <div v-if="data.total > 0">
      <el-row :gutter="20">
        <el-col :span="6" v-for="item in data.tableData" :key="item.id">
          <div @click="router.push('/front/goodsDetail?id=' + item.id)" class="card goods-item" style="width: 100%;margin-bottom: 20px;padding: 0">
            <img :src="item.img" alt="" style="width: 100%; border-radius: 5px 5px 0 0;">
            <div style="padding: 10px">
              <div class="line1" style="font-size: 16px; font-weight: bold;margin-bottom: 5px">{{item.name}}</div>
              <div>
                <span style="font-size: 15px;color: red;font-weight: bolder">¥</span><b style="color: red;font-size: 20px">{{item.price}}</b>
                <span style="margin-left: 5px;color: grey;">销量：{{item.saleCount}}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <!-- 分页组件 -->
      <div>
        <el-pagination style="background-color: white;width: fit-content;padding: 10px 5px;border-radius: 5px" @current-change="load" layout="total, prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
      </div>
    </div>
    <div v-else>
      <div style="text-align: center;margin-top: 20px">暂无商品</div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import router from "@/router";
import request from "@/utils/request";
import {useRouter} from "vue-router";


const data = reactive({
  name: router.currentRoute.value.query.name,
  tableData: [],
  pageNum: 1,
  pageSize: 4,
  total: 0,
  categoryId: null,
  categoryList: []
})

const loadByCategory = (categoryId) => {
  data.categoryId = categoryId
  load()
}

/**
 * 分页查询商品数据
 * 向后端API发起分页查询请求，获取商品列表数据
 * 根据当前页码、每页数量、商品名称和分类ID等条件进行查询
 */
const load = () => {
  request.get('/goods/selectPage', {
    params: {
      pageNum: data.pageNum,        // 当前页码
      pageSize: data.pageSize,      // 每页显示数量
      name: data.name,              // 商品名称查询条件
      categoryId: data.categoryId   // 商品分类ID查询条件
    }
  }).then(res => {
    data.tableData = res.data?.list    // 更新表格数据
    data.total = res.data?.total       // 更新总记录数
  })
}
load()

/**
 * 加载商品分类列表
 * 向后端API请求所有商品分类信息，用于查询条件的选择
 */
const loadCategoryList = () => {
  request.get('/category/selectAll').then(res => {
    data.categoryList = res.data    // 更新分类列表数据

  })
}
loadCategoryList()

/**
 * 清除URL参数
 * 从当前浏览器URL中移除查询参数和锚点参数，
 * 并使用history API更新URL而不刷新页面
 */
const clearPathParam = () => {
  let url = location.href
  url = url.replace(/(\?|#)[^'"]*/, '');           // 去除参数
  window.history.pushState({},0, url);
}
clearPathParam()

</script>

<style scoped>
.active {
  font-weight: bold;
  color: green;
  border-bottom: 2px solid green;
}

/* 商品卡片基础样式 */
.goods-item {
  transition: all 0.25s ease;          /* 添加过渡动画，使悬停效果更平滑 */
}

/* 商品卡片悬停效果 */
.goods-item:hover {
  border-radius: 5px;                 /* 悬停时设置圆角，使边框呈现圆角效果 */
  transform: scale(1.03) translateY(-0.1px);  /* 悬停时轻微放大并略微上移，创造浮起效果 */
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1); /* 悬停时添加阴影效果，增强立体感 */
  z-index: 10;                       /* 悬停时提升层级，确保卡片显示在其他元素之上 */
}
</style>