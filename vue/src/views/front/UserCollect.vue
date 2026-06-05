<template>
  <div class="front-container">
    <!-- 收藏商品列表标题 -->
    <div style="font-size: 20px; font-weight: bold;margin-bottom: 20px">收藏商品列表（{{data.total}}）</div>
    <!-- 收藏商品列表 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in data.tableData" :key="item.id">
        <div @click="router.push('/front/goodsDetail?id=' + item.goodsId)" class="card goods-item" style="width: 100%;margin-bottom: 20px;padding: 0;cursor: pointer;">
          <img :src="item.goodsImg" alt="" style="width: 100%; border-radius: 5px 5px 0 0;">
          <div style="padding: 10px">
            <!-- 第一行商品名称 -->
            <div class="line1" style="font-size: 16px; font-weight: bold;margin-bottom: 5px">{{item.goodsName}}</div>
            <!-- 第二行商品价格和收藏按钮 -->
            <div style="display: flex;align-items: center;justify-content: space-between;">
              <!-- 左边商品价格 -->
              <div><span style="font-size: 15px;color: red;font-weight: bolder">¥</span><b style="color: red;font-size: 20px">{{item.goodsPrice}}</b></div>
              <!-- 右边取消按钮 -->
              <el-button type="danger" size="small" @click.stop="cancel(item.id)">取消收藏</el-button>
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
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request";
import router from "@/router";
import {ElMessage} from "element-plus";


const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {},
  tableData: []
})

// 分页查询
const load = () => {
  request.get('/collect/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      userId: data.user.id
    }
  }).then(res => {
    console.log('后端返回的 res:', res)
    console.log('res.data:', res.data)
    data.tableData = res.data?.list
    console.log('tableData:', data.tableData)
    data.total = res.data?.total
  })
}
load()

//取消收藏
const cancel = (collectId) => {
  request.delete('/collect/delete/' + collectId).then(res => {
    if (res.code === '200') {
      ElMessage.success('取消收藏成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}
</script>