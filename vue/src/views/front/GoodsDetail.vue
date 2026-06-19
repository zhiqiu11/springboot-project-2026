<template>
  <div class="front-container" style="width: 50%;">
    <div class="card" style="padding: 20px;display: flex;grid-gap: 20px;margin-bottom: 20px;">
      <!-- 左边商品图片 -->
      <img :src="data.goods.img" alt="" style="width: 300px;height: 300px">
      <!-- 右边商品信息 -->
      <div style="flex:1">
        <!-- 第一行商品信息 -->
        <div style="display: flex;align-items: center;justify-content: space-between;margin-bottom: 20px">
          <!-- 左边商品名称和推荐状态 -->
          <div style="font-size: 20px;max-width: 80%;">
            <el-tag type="danger" v-if="data.goods.recommend === '是'" style="font-size: 15px;bottom: 2.5px;position: relative;background-color: red;color: white;">推荐</el-tag>
            {{data.goods.name}}
          </div>
          <!-- 右边收藏图标 -->
          <div style="width: fit-content;cursor: pointer;" @click="addCollect" v-if="!data.userCollect?.id">
            <el-icon style="font-size: 20px;top: 5px;position: relative;"><Star /></el-icon>收藏
          </div>
          <div style="width: fit-content;cursor: pointer;color: orange" @click="removeCollect" v-if="data.userCollect?.id">
            <el-icon style="font-size: 20px;top: 5px;position: relative;"><StarFilled /></el-icon>取消收藏
          </div>
        </div>
        <!-- 第二行商品市场情况 -->
        <div style="margin-bottom: 20px;">
          <!-- ===== 外层容器：渐变背景 + 保留原有布局 ===== -->
          <!-- 秒杀商品：棕色渐变 -->
          <div v-if="data.goods.hasFlash === '是'"
               style="background-image: linear-gradient(to right, #85411f, #2c1e1e);
              padding: 12px 16px; border-radius: 8px;
              display: flex; align-items: center; flex-wrap: wrap; gap: 10px;">
            <!-- 价格部分（保留原有大红色字体） -->
            <span style="font-size: 20px; color: white; font-weight: bolder;">¥</span>
            <b style="font-size: 25px; color: white;">{{ data.goods.flashPrice }}</b>
            <!-- 原价（灰色删除线） -->
            <span style="font-size: 16px; color: rgba(255,255,255,0.7); text-decoration: line-through; margin-left: 5px;">
      原价 ¥{{ data.goods.price }}
    </span>
            <!-- 销量和库存（保留原有灰色，但在白色背景上可能看不清，改成浅灰色） -->
            <span style="color: rgba(255,255,255,0.9); margin-left: auto;">累计销量：{{ data.goods.saleCount }}</span>
            <span style="color: rgba(255,255,255,0.9);">库存：{{ data.goods.store }}</span>
          </div>

          <!-- 团购商品：红色渐变 -->
          <div v-else-if="data.goods.hasGroup === '是'"
               style="background-image: linear-gradient(to right, #f83939, #f52593);
              padding: 12px 16px; border-radius: 8px;
              display: flex; align-items: center; flex-wrap: wrap; gap: 10px;">
            <span style="font-size: 20px; color: white; font-weight: bolder;">¥</span>
            <b style="font-size: 25px; color: white;">{{ data.goods.groupPrice }}</b>
            <span style="font-size: 16px; color: rgba(255,255,255,0.7); text-decoration: line-through; margin-left: 5px;">
      原价 ¥{{ data.goods.price }}
    </span>
            <span style="color: rgba(255,255,255,0.9); margin-left: auto;">累计销量：{{ data.goods.saleCount }}</span>
            <span style="color: rgba(255,255,255,0.9);">库存：{{ data.goods.store }}</span>
          </div>

          <!-- 普通商品：保持原有的纯红色大字风格（无渐变） -->
          <div v-else
               style="background-image: linear-gradient(to right, #169823, #33c3ba);
              padding: 12px 16px; border-radius: 8px;
              display: flex; align-items: center; flex-wrap: wrap; gap: 10px;">
            <span style="font-size: 20px; color: white; font-weight: bolder;">¥</span>
            <b style="font-size: 25px; color: white;">{{ data.goods.price }}</b>
            <span style="color: rgba(255,255,255,0.9); margin-left: auto;">累计销量：{{ data.goods.saleCount }}</span>
            <span style="color: rgba(255,255,255,0.9);">库存：{{ data.goods.store }}</span>
          </div>
        </div>
        <!-- 第三行商品描述 -->
        <div style="margin-bottom: 20px;">
          <div style="padding: 10px;background-color: lightgrey;border-radius: 5px;text-align: justify">{{data.goods.description}}</div>
        </div>
        <!-- 第四行购买数量 -->
        <div style="margin-bottom: 20px;">
          <el-input-number v-model="data.num" :min="1" :max="data.goods.store" style="width: 150px;height: 40px" />
          <el-button @click="addCart"  type="warning" style="margin-left: 10px;height: 40px;">加入购物车</el-button>
          <el-button @click="startOrder('flash')" type="danger" style="margin-left: 10px;height: 40px;" v-if="data.goods.hasFlash === '是'">立即秒杀</el-button>
          <el-button @click="startOrder('group')" type="danger" style="margin-left: 10px;height: 40px;" v-else-if="data.goods.hasGroup === '是'">参与团购</el-button>
          <el-button @click="startOrder('normal')" type="danger" style="margin-left: 10px;height: 40px;" v-else>立即购买</el-button>
        </div>
        <!-- 第五行购买简介 -->
        <div style="margin-top: 10px; color: #666;">校园小卖部销售并发货的商品，由小卖部提供发票和相应的售后服务，请您放心购买</div>
      </div>
    </div>
    <!-- 点击购买时的弹窗 -->
    <el-dialog title="购买信息" width="30%" v-model="data.formVisible" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="80px" style="padding-right: 30px">
        <!-- 选择配送类型 -->
        <el-form-item label="配送类型" prop="deliverType">
          <el-radio-group v-model="data.form.deliverType" autocomplete="off" >
            <el-radio-button value="自提" label="自提">到店自提</el-radio-button>
            <el-radio-button value="配送" label="配送">配送到家</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <!-- 选择收获地址 -->
        <el-form-item label="收获地址" prop="address" v-if="data.form.deliverType === '配送'">
          <el-input v-model="data.form.address" type="textarea" :rows="3" placeholder="请输入收获地址，包括联系人姓名、手机号、详细地址" />
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="data.formVisible = false">取 消</el-button>
        <el-button type="primary" @click="addOrder">确 认</el-button>
      </span>
      </template>
    </el-dialog>

    <!-- 详情页面 -->
    <div class="card" style="padding: 20px;margin-bottom: 20px;">
      <!-- 第一行标题 -->
      <div style="font-size: 20px;padding-bottom: 10px;border-bottom: 1px solid grey;">
        <span :class="{'current-active': data.current === '商品详情'}" @click="changeTab('商品详情')" style="cursor: pointer">商品详情</span>
        <span :class="{'current-active': data.current === '商品评价'}" @click="changeTab('商品评价')" style="margin-left: 20px;cursor: pointer">商品评价</span>
      </div>
      <!-- 第二行商品介绍 -->
      <!-- 商品详情 -->
      <div v-if="data.current === '商品详情'" style="padding:10px;" v-html="data.goods.content"></div>
      <!-- 商品评论 -->
      <div v-if="data.current === '商品评价'" style="padding:10px;min-height: 600px;">
        <div v-if="data.commentList.length === 0" style="text-align: center;padding: 50px;color: grey">暂无评论...</div>
        <div v-if="data.commentList.length > 0">
          <!-- 商品评论列表 -->
          <div v-for="(item, index) in data.commentList"
               :key = "item.id"
               style="display: flex;grid-gap: 10px;padding: 10px 0;border-bottom: 1px solid lightgrey;"
               :style="{'borderWidth': index === data.commentList.length - 1 ? 0 : '1px'}">
            <!-- 左边评价人信息 -->
            <img :src="item.userAvatar" alt="评价人头像" style="width: 50px;height: 50px;border-radius: 50%;margin-right: 10px;">
            <!-- 右边评价内容 -->
            <div style="flex: 1;">
              <!-- 上面评价人姓名和时间 -->
              <div><span>{{ item.userName }}</span>
                <span style="color: #666; font-size: 13px; margin-left: 10px">{{ item.time }}</span>
              </div>
              <!-- 下面评价分数内容 -->
              <div><el-rate v-model="item.score" show-score allow-half :disabled="true"></el-rate></div>
              <div>{{ item.content }}</div>
            </div>
          </div>
          <!-- 分页按钮 -->
          <div style="margin-top: 20px;">
            <el-pagination  @current-change="loadComment" layout="total, prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import router from "@/router";
import request from "@/utils/request";
import {ElMessage} from "element-plus";
const formRef = ref()


const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  id: router.currentRoute.value.query.id,
  goods: {},
  num: 1,
  pageNum: 1,
  pageSize: 5,
  total: 0,
  current: '商品详情',
  commentList: [],
  userCollect: {},
  form: {},
  formVisible: false,
  orderType: null,  // 'flash' | 'group' | 'normal' —— 标记当前订单类型
  rules: {
    deliverType: [{ required: true, message: '请选择配送类型', trigger: 'change' }],
    address: [{ required: true, message: '请输入收获地址', trigger: 'blur' }]
  }
})

// 分页查询
const loadComment = () => {
  request.get('/comment/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      goodsId: data.id
    }
  }).then(res => {
    data.commentList = res.data?.list
    data.total = res.data?.total
  })
}
loadComment()


// 统一下单入口：秒杀/团购/普通都先打开配送弹窗
const startOrder = (type) => {
  data.orderType = type
  data.formVisible = true
  data.form = {}
}

// 弹窗确认按钮——根据 orderType 调用不同接口
const addOrder = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.form.userId = data.user.id
      data.form.cartList = [{goodsId: data.id, num: data.num}]

      let url = '/orders/add'         // 默认：普通下单 / 团购
      if (data.orderType === 'flash') {
        url = '/orders/addFlashOrder' // 秒杀接口
      } else if (data.orderType === 'group') {
        url = '/orders/addGroupOrder' // 团购接口
      }

      request.post(url, data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('下单成功')
          data.formVisible = false
          load()
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

//加入购物车
const addCart = () => {
  // 添加参数校验
  if (!data.id || !data.user.id) {
    ElMessage.error('参数错误')
    return
  }
  request.post('/cart/add', {
    userId: data.user.id,
    goodsId: data.id,
    num: data.num
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('加入购物车成功')

    } else {
      ElMessage.error(res.msg)
    }
  })
}

//检测当前商品是否被收藏
const loadCollect = () => {
  request.get('/collect/selectAll', {
    params: {
      userId: data.user.id,
      goodsId: data.id
    }
  }).then(res => {
    if (res.data?.length > 0) {//查询到数据，用户已经拿到了
      data.userCollect = res.data[0]
    } else {
      data.userCollect = {}
    }
  })
}
loadCollect()

//取消收藏
const removeCollect = () => {
  request.delete('/collect/delete/' + data.userCollect.id).then(res => {
    if (res.code === '200') {
      ElMessage.success('取消收藏成功')
      loadCollect()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 新增收藏
const addCollect = () => {
  request.post('/collect/add', {
    userId: data.user.id,
    goodsId: data.id
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('收藏成功')
      loadCollect()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const changeTab = (tabName) => {
  data.current = tabName
}

const load = () => {
  request.get('/goods/selectById/' + data.id).then(res => {
    data.goods = res.data
  })
}
load()

</script>

<style>
.current-active {
  color: red;
  border-bottom: 2px solid red;
  padding-bottom: 10px
}
</style>