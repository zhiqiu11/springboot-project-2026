<template>
  <div class="front-container" style="width: 80%;">
    <div style="margin-bottom: 10px;">
      <el-input clearable @clear="load" v-model="data.orderNo" style="width: 400px;height: 40px; margin-right: 10px" placeholder="请输入订单号查询"></el-input>
      <el-input clearable @clear="load" v-model="data.goodsName" style="width: 400px;height: 40px; margin-right: 10px" placeholder="请输入商品名称查询"></el-input>
      <el-button type="primary" @click="load" style="height: 40px;">查询</el-button>
    </div>

    <div class="card">
      <el-table :data="data.tableData" stripe :cell-style="{ background: '#d3e5fa' }" default-expand-all>
        <!-- 下拉订单详情页 -->
        <el-table-column type="expand">
          <template #default="props">
            <div style="padding: 10px">
              <el-table :data="props.row.orderDetailList" stripe border >
                <el-table-column prop="goodsImg" label="商品图片" width="100px">
                  <template #default="scope">
                    <img :src="scope.row.goodsImg" alt="" style="width: 50px; height: 50px; display: flex; margin: 0 auto;">
                  </template>
                </el-table-column>
                <el-table-column prop="goodsName" label="商品名称" show-overflow-tooltip></el-table-column>
                <el-table-column prop="goodsPrice" label="商品单价" width="100px"></el-table-column>
                <el-table-column prop="num" label="商品数量" width="100px"></el-table-column>
                <el-table-column label="小计" width="150px">
                  <template #default="scope">
                    <b style="color: red;">{{ (scope.row.goodsPrice * scope.row.num).toFixed(2) }}元</b>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120px">
                  <template #default="scope">
                    <el-button @click="handleComment(scope.row)" type="success" v-if="props.row.status === '已完成'">评价订单</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

          </template>
        </el-table-column>

        <el-table-column prop="orderNo" label="订单编号">
          <template #default="scope">
            <b style="color: black;">{{ scope.row.orderNo }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="deliverType" label="配送类型"></el-table-column>
        <el-table-column prop="total" label="总价格">
          <template #default="scope">
            <b style="color: red;">{{ scope.row.total}}元</b>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag type="danger" v-if="scope.row.status === '已取消'">已取消</el-tag>
            <el-tag type="warning" v-else-if="scope.row.status === '待接单'">待接单</el-tag>
            <el-tag type="info" v-else-if="scope.row.status === '已配送'">已配送</el-tag>
            <el-tag type="info" v-else-if="scope.row.status === '已出货'">已出货</el-tag>
            <el-tag type="success" v-else-if="scope.row.status === '已完成'">已完成</el-tag>
            <el-tag type="primary" v-else>{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="下单时间"></el-table-column>
        <el-table-column prop="address" label="配送地址" width="150px"></el-table-column>
        <el-table-column prop="deliver" label="配送信息" width="150px"></el-table-column>
        <el-table-column label="订单操作" align="center" width="150">
          <template #default="scope">
            <el-button type="danger" v-if="scope.row.status === '待接单'" @click="handleCancel(scope.row)">取消订单</el-button>
            <el-button @click="handleConfirm(scope.row)" type="primary" v-if="scope.row.status === '已出货' || scope.row.status === '已配送'">确认收货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页按钮 -->
      <div style="padding: 20px">
        <el-pagination  @current-change="load" background layout="total, prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
      </div>

      <!-- 新增/编辑弹窗 -->
      <el-dialog title="评价信息" width="30%" v-model="data.formVisible" :close-on-click-modal="false" destroy-on-close>
        <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="80px" style="padding-right: 30px;padding-top: 20px">
          <el-form-item label="评分" prop="score">
            <el-rate v-model="data.form.score" allow-half show-score></el-rate>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input type="textarea" :rows="3" v-model="data.form.content" autocomplete="off" placeholder="请输入评价内容"/>
          </el-form-item>
        </el-form>
        <template #footer>
      <span class="dialog-footer">
        <el-button @click="data.formVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">保 存</el-button>
      </span>
        </template>
      </el-dialog>

    </div>

  </div>
</template>

<script setup>
import request from "@/utils/request";
import {reactive, ref} from "vue";
import {ElMessageBox, ElMessage} from "element-plus";

const formRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  pageNum: 1,
  pageSize: 3,
  total: 0,
  formVisible: false,
  form: {},
  tableData: [],
  orderNo: null,
  goodsName: null,
  rules:{
    score:[
      { required: true, message: '请选择评分', trigger: 'change' }
    ],
    content:[
      { required: true, message: '请输入内容', trigger: 'blur' }
    ]
  }
})

// 分页查询
const load = () => {
  request.get('/orders/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      orderNo: data.orderNo,
      goodsName: data.goodsName
    }
  }).then(res => {
    data.tableData = res.data?.list
    data.total = res.data?.total
  })
}
load()

// 确认取消订单
const handleCancel = (row) => {
  ElMessageBox.confirm('您确定确认取消订单吗?', '同意确认', { type: 'warning' }).then(res => {
    data.form = row
    data.form.status = '已取消'
    updateOrder()
  }).catch(err => {
    console.log(err)
  })
}

// 确认收货
const handleConfirm = (row) => {
  ElMessageBox.confirm('您确定确认收货吗?', '同意确认', { type: 'warning' }).then(res => {
    data.form = row
    data.form.status = '已完成'
    updateOrder()
  }).catch(err => {
    console.log(err)
  })
}

// 编辑保存
const updateOrder = () => {

  request.put('/orders/update', data.form).then(res => {
    if (res.code === '200') {
      load()
      ElMessage.success('操作成功')
      data.formVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 新增保存
const addComment = () => {
  request.post('/comment/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 编辑保存
const updateComment = () => {
  request.put('/comment/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 新增
const handleComment = (row) => {
  request.get('/comment/selectAll', {
    params: {
      orderId: row.orderId,
      goodsId: row.goodsId
    }
  }).then(res => {
    data.form = res.data?.length > 0 ? res.data[0] : { orderId: row.orderId, goodsId: row.goodsId, userId: data.user.id }
    data.formVisible = true
  })
}

// 弹窗保存
const save = () => {
  formRef.value.validate(valid =>{
    if (valid) {
      // data.form有id就是更新，没有就是新增
      data.form.id ? updateComment() : addComment()
    }
  })
}

</script>

<style scoped>
.el-tag {
  font-weight: bold;
}

</style>