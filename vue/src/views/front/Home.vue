<template>



  <div class="front-container">
    <!-- 第一行开始 -->
    <div class="card" style="display: flex; grid-gap: 20px; margin-bottom: 10px;">
      <!-- 左侧轮播图 -->
      <div style="flex: 1">
        <el-carousel height="480px">
          <el-carousel-item v-for="item in data.carouselList" :key="item.id">
            <img @click="router.push('/front/goodsDetail?id=' + item.goodsId)" :src="item.img" alt="" style="width: 100%;height: 480px;cursor: pointer;">
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 右侧推荐商品 -->
      <div class="card" style="width: 540px;background-color: cornsilk" >
        <div style="display: flex; align-items: center; padding-bottom: 5px; border-bottom: 2px solid red">
          <img :src="loveImg" alt="" style="width: 25px;">
          <div style="color: red;font-weight: bold; font-size: 20px; margin-left: 5px;">为你推荐</div>
        </div>
        <div style="padding: 20px 0">
          <div class="top-item"
               style="display:
               flex; grid-gap: 10px;margin-bottom: 10px;cursor: pointer;"
               v-for="item in data.recommendGoods"
               :key="item.id"
               @click="router.push('/front/goodsDetail?id=' + item.id)">
            <img :src="item.img" alt="" style="width: 85px; height: 85px; border-radius: 5px 5px 0 0;">
            <div style="flex: 1">
              <div class="line2" style="font-size: 16px; font-weight: bold;">{{item.name}}</div>
              <span style="font-size: 15px;color: red;font-weight: bolder">¥</span><b style="color: red;font-size: 20px">{{item.price}}</b>
              <span style="margin-left: 5px;color: grey;">销量：{{item.saleCount}}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 第一行结束 -->

    <div class="card" style="padding: 20px">
      <!-- 第二行开始 -->
      <div style="display: flex; align-items: flex-end; padding-bottom: 5px; border-bottom: 2px solid red;margin-bottom: 10px;">
        <div style="flex: 1; display: flex; align-items: center;">
          <img :src="hotImg" alt="" style="width: 25px;">
          <div style="color: red;font-weight: bold; font-size: 20px; margin-left: 5px;">热销商品</div>
        </div>
        <div @click="router.push('/front/goods')" style="cursor: pointer; font-size: 13px; font-weight: bold;">查看更多</div>
      </div>
      <div style="margin-bottom: 40px;">
        <el-row :gutter="20">
          <el-col :span="6" v-for="item in data.hotGoods" :key="item.id">
            <div @click="router.push('/front/goodsDetail?id=' + item.id)" class="item" style="width: 100%;">
              <img :src="item.img" alt="" style="width: 100%; border-radius: 5px 5px 0 0;">
              <div style="padding: 10px">
                <div class="line2" style="font-size: 16px; font-weight: bold;">{{item.name}}</div>
                <div>
                  <span style="font-size: 15px;color: red;font-weight: bolder">¥</span><b style="color: red;font-size: 20px">{{item.price}}</b>
                  <span style="margin-left: 5px;color: grey;">销量：{{item.saleCount}}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      <!-- 第二行结束 -->

      <!-- 第三行开始 -->
      <div style="display: flex; align-items: flex-end; padding-bottom: 5px; border-bottom: 2px solid red;margin-bottom: 10px;">
        <div style="flex: 1; display: flex; align-items: center;">
          <img :src="newImg" alt="" style="width: 25px;">
          <div style="color: red;font-weight: bold; font-size: 20px; margin-left: 5px;">最新上架</div>
        </div>
        <div @click="router.push('/front/goods')" style="cursor: pointer; font-size: 13px; font-weight: bold;">查看更多</div>
      </div>
      <div>
        <el-row :gutter="20">
          <el-col :span="6" v-for="item in data.newGoods" :key="item.id">
            <div  @click="router.push('/front/goodsDetail?id=' + item.id)" class="item" style="width: 100%;">
              <img :src="item.img" alt="" style="width: 100%; border-radius: 5px 5px 0 0;">
              <div style="padding: 10px">
                <div class="line2" style="font-size: 16px; font-weight: bold;">{{item.name}}</div>
                <div>
                  <span style="font-size: 15px;color: red;font-weight: bolder">¥</span><b style="color: red;font-size: 20px">{{item.price}}</b>
                  <span style="margin-left: 5px;color: grey;">销量：{{item.saleCount}}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
    <!-- 第三行结束 -->
  </div>
</template>

<script setup>
import {reactive} from 'vue'
import request from "@/utils/request";
import loveImg from '@/assets/imgs/love.png'
import hotImg from '@/assets/imgs/hot.png'
import newImg from '@/assets/imgs/new.png'
import router from "@/router";


const data = reactive({
  carouselList: [],
  hotGoods: [],
  newGoods: [],
  recommendGoods: []
})

request.get('/carousel/selectAll').then(res => {
  data.carouselList = res.data
})

request.get('/goods/selectAll', {
  params: {
    status: '上架'
  }
}).then(res => {
  data.hotGoods = res.data.sort((v1, v2) => v2.saleCount - v1.saleCount).splice(0, 4)
})

request.get('/goods/selectAll', {
  params: {
    status: '上架'
  }
}).then(res => {
  data.newGoods = res.data.splice(0, 4)
})

request.get('/goods/selectAll', {
  params: {
    status: '上架'
  }
}).then(res => {
  data.recommendGoods = res.data.filter(item => item.recommend === '是').splice(0, 4)
})

</script>

<style>
/* 商品卡片基础样式 */
.item {
  cursor: pointer;                    /* 鼠标悬停时显示手型光标，提示可点击 */
  border: 1px solid transparent;      /* 预设透明边框，防止悬停时因边框出现导致的尺寸跳动 */
  overflow: hidden;                   /* 隐藏超出容器边界的元素内容 */
}
/* 商品卡片悬停效果 */
.item:hover {
  border-radius: 5px;                 /* 悬停时设置圆角，使边框呈现圆角效果 */
  border: 1px solid #ff6b6b;          /* 悬停时边框颜色变为红色系，增强视觉反馈 */
  transform: scale(1.01) translateY(-0.1px);  /* 悬停时轻微放大并略微上移，创造浮起效果 */
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1); /* 悬停时添加阴影效果，增强立体感 */
  z-index: 10;                       /* 悬停时提升层级，确保卡片显示在其他元素之上 */
}
.top-item:hover {
  color: red;
}
</style>