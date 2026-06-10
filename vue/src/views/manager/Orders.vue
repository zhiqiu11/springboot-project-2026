<template>
  <div>

    <div class="card" style="margin-bottom: 5px;">
      <el-input v-model="data.orderNo" style="width: 300px; margin-right: 10px" placeholder="请输入订单号查询"></el-input>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="info" style="margin: 0 10px" @click="reset">重置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table :data="data.tableData" stripe :header-cell-style="{ background: '#d3e5fa' }">
        <el-table-column type="expand">
          <template #default="props">
            <div style="padding: 10px">
              <el-table :data="props.row.orderDetailList" stripe border>
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
              </el-table>
            </div>

          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号"></el-table-column>
        <el-table-column prop="deliverType" label="配送类型"></el-table-column>
        <el-table-column prop="total" label="总价格">
          <template #default="scope">
            <b style="color: red;">{{ scope.row.total}}元</b>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="下单人姓名"></el-table-column>
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
        <el-table-column label="订单操作" align="center" width="160">
          <template #default="scope">
            <el-button v-if="scope.row.deliverType === '自提' && scope.row.status === '待接单'" type="primary" @click="handleOut(scope.row)">出货</el-button>
            <el-button v-if="scope.row.deliverType === '配送' && scope.row.status === '待接单'" type="primary" @click="handleDelivery(scope.row)">配送</el-button>
          </template>
        </el-table-column>
        <el-table-column label="删除" align="center" width="160">
          <template #default="scope">
            <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <el-pagination  @current-change="load" background layout="total, prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
    </div>
    <!-- 配送弹窗 -->
    <el-dialog title="订单信息" width="30%" v-model="data.formVisible" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="80px" style="padding-right: 30px;padding-top: 20px">
        <el-form-item label="配送信息" prop="deliver">
          <el-input placeholder="请输入配送员姓名、联系方式" type="textarea" :rows="3" v-model="data.form.deliver" autocomplete="off" />
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
</template>

<script setup>
import request from "@/utils/request";
import {reactive, ref} from "vue";
import {ElMessageBox, ElMessage} from "element-plus";

const formRef = ref()

const data = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {},
  tableData: [],
  name: null,
  rules:{
    deliver:[
      { required: true, message: '请输入配送信息', trigger: 'blur' }
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
      role: '管理员'
    }
  }).then(res => {
    data.tableData = res.data?.list
    data.total = res.data?.total
  })
}
load()

// 配送
const handleDelivery = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

// 出货
const handleOut = (row) => {
  ElMessageBox.confirm('您确定确认出货订单吗?', '同意确认', { type: 'warning' }).then(res => {
    data.form = row
    data.form.status = '已出货'
    request.put('/orders/update', data.form).then(res => {
      if (res.code === '200') {
        load()
        ElMessage.success('操作成功')
        data.formVisible = false
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {
    console.log(err)
  })
}

// 编辑
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

// 新增保存
const add = () => {
  request.post('/orders/add', data.form).then(res => {
    if (res.code === '200') {
      load()
      ElMessage.success('操作成功')
      data.formVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 编辑保存
const update = () => {
  // 更新订单状态
  if (data.form.deliverType === '自提') {
    data.form.status = '已出货'
  }
  if (data.form.deliverType === '配送') {
    data.form.status = '已配送'
  }
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

// 弹窗保存
const save = () => {
  formRef.value.validate(valid =>{
    if (valid) {
      // data.form有id就是更新，没有就是新增
      data.form.id ? update() : add()
    }
  })
}

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗?', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/orders/delete/' + id).then(res => {
      if (res.code === '200') {
        load()
        ElMessage.success('操作成功')
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {})
}

// 重置
const reset = () => {
  data.orderNo = null
  load()
}

</script>

<style scoped>
.el-tag {
  font-weight: bold;
}

</style>