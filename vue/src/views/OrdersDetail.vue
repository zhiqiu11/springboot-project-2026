<template>
  <div class="front-container" style="width: 50%;">
    <div class="card" style="padding: 20px;display: flex;grid-gap: 20px;margin-bottom: 20px;">
      <el-row :gutter="10">
        <img :src="data.ordersData.goodsImg" alt="" style="width: 150px; height: 150px; border-radius: 10px; border: 1px solid #cccccc">
        <el-col :span="20" style="flex:1">
          <div style="font-size: 20px; font-weight: bold; border-bottom: 10px; line-height: 30px">{{ data.ordersData.goodsName }}</div>
          <div style="margin-top: 15px; font-size: 15px">订单编号： {{ data.ordersData.orderNo }}</div>
          <div style="margin-top: 15px; font-size: 15px">创建时间： {{ data.ordersData.time }}</div>
          <div style="margin-top: 15px; font-size: 15px">订单状态：
            <el-tag type="danger" v-if="data.ordersData.status === '待支付'">待支付</el-tag>
            <el-tag type="warning" v-else-if="data.ordersData.status === '待接单'">待接单</el-tag>
            <el-tag type="info" v-else-if="data.ordersData.status === '已配送'">已配送</el-tag>
            <el-tag type="info" v-else-if="data.ordersData.status === '已出货'">已出货</el-tag>
            <el-tag type="success" v-else-if="data.ordersData.status === '已完成'">已完成</el-tag>
            <el-tag type="danger" v-else-if="data.ordersData.status === '已取消'">已取消</el-tag>
            <el-tag type="primary" v-else>{{ data.ordersData.status || '-' }}</el-tag>
          </div>
          <div style="margin-top: 15px; font-size: 15px">商品价格： ￥{{ data.ordersData.goodsPrice }}</div>
          <div style="margin-top: 15px; font-size: 15px">购买数量： {{ data.ordersData.num }}</div>
          <div style="margin-top: 15px; font-size: 15px">订单总价： <span style="font-size: 18px; color: red">￥{{ data.ordersData.total }}</span></div>
          <div style="margin-top: 15px; display: flex; align-items: center; gap: 20px;">
            <div>
              <el-button v-if="data.ordersData.status === '待支付'" type="primary" style="padding: 20px 30px;margin-left: 5px" @click="pay">去支付</el-button>
              <el-button v-else-if="data.ordersData.status === '已取消'" :disabled="true" type="danger" style="padding: 20px 30px;margin-left: 5px">已取消</el-button>
              <el-button  v-else :disabled="true" type="success" style="padding: 20px 30px; margin-left: 5px">已付款</el-button>
            </div>
            
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  ordersId: router.currentRoute.value.query.id,
  ordersData: {},
})

const loadOrders = () => {
  request.get('/orders/selectById/' + data.ordersId).then(res => {
    if (res.code === '200') {
      data.ordersData = res.data

      // 取出订单详情（第一个商品）
      const detail = data.ordersData.orderDetailList?.[0] || {}
      // 把详情信息合并到 ordersData 中
      data.ordersData = {
        ...data.ordersData,
        goodsImg: detail.goodsImg,
        goodsName: detail.goodsName,
        goodsPrice: detail.goodsPrice,
        num: detail.num,
        goodsId: detail.goodsId    // 用于参与团购时传参
      }
      

    } else {
      ElMessage.error(res.msg)
    }
  })
}


const pay = () => {
  request.post('/userPay/pay', {
    id: data.ordersData.id,
    total: data.ordersData.total,
    status: data.ordersData.status,
    userId: data.ordersData.userId
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('支付成功')
      loadOrders()  // 刷新订单状态和拼团信息
    } else {
      ElMessage.error(res.msg)
    }
  })
}



loadOrders()

</script>

<style scope>
.overflow {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis
}
</style>