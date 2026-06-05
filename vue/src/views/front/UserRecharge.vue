<template>
  <div class="front-container">
    <!-- 用户充值记录标题 -->
    <div class="card" style="padding: 20px">
      <div style="font-size: 20px; font-weight: bold;margin-bottom: 20px">我的充值记录（{{data.total}}条）</div>
      <!-- 查询条件 -->
      <div style="margin-bottom: 20px;display: flex;align-items: center;justify-content: space-between;" >
        <!-- 左边查询组件 -->
        <div style="flex:1">
          <el-date-picker style="width: 300px;margin-right: 10px" v-model="data.time" type="date" placeholder="请输入日期查询" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="info" style="margin: 0 10px" @click="reset">重置</el-button>
        </div>
        <!-- 新增充值界面 -->
        <!-- 当前账户余额 -->
        <b style="margin-left: 20px;color: red;top: 5px;position: relative;">当前账户余额：{{data.user.account}}元</b>
        <!-- 新增充值按钮 -->
        <el-button type="primary" style="margin-left: 10px" @click="handleAdd">新增充值</el-button>
      </div>

      <div style="margin-bottom: 20px">
        <el-table :data="data.tableData" stripe>
          <el-table-column prop="money" label="充值金额">
            <template #default="scope">
              <b style="color: red">{{scope.row.money}}元</b>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="支付方式"></el-table-column>
          <el-table-column prop="time" label="充值时间"></el-table-column>
        </el-table>
      </div>

      <div>
        <el-pagination  @current-change="load" background layout="total, prev, pager, next" v-model:page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total"/>
      </div>
    </div>
    <!-- 新增充值弹窗 -->
    <el-dialog title="用户充值" width="30%" v-model="data.formVisible" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="80px" style="padding-right: 30px;padding-top: 20px">
        <!-- 第一行充值金额 -->
        <el-form-item label="充值金额" prop="money">
          <el-input-number style="width: 200px" :min="1" v-model="data.form.money" autocomplete="off" />
        </el-form-item>
        <!-- 第二行支付方式 -->
        <el-form-item label="支付方式" prop="type" style="margin-top: 20px;">
          <el-radio-group v-model="data.form.type">
            <el-radio value="支付宝"><img src="@/assets/imgs/alipay.png" alt="支付宝" style="width: 100px;margin-right: 5px;"></el-radio>
            <el-radio value="微信支付"><img src="@/assets/imgs/wechat.png" alt="微信" style="width: 100px;margin-right: 5px;"></el-radio>
          </el-radio-group>
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
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {},
  tableData: [],
  time: null,
  rules: {
    money: [
      { required: true, message: '请输入充值金额', trigger: 'blur' }
    ],
    userId: [
      { required: true, message: '请选择充值人', trigger: 'change' }
    ],
    type: [
      { required: true, message: '请输入支付方式', trigger: 'blur' }
    ],
    time: [
      { required: true, message: '请选择充值时间', trigger: 'blur' }
    ]
  }
})

const loadAccount = () =>{
  request.get('/user/selectById/' + data.user.id).then(res => {
    data.user.account = res.data.account
  })
}
loadAccount()

// 分页查询
const load = () => {
  request.get('/recharge/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      userId: data.user.id,
      time: data.time
    }
  }).then(res => {
    data.tableData = res.data?.list
    data.total = res.data?.total
  })
}
load()

// 新增
const handleAdd = () => {
  data.form = {money: 1,type: '微信支付'}
  data.formVisible = true
}

// 编辑
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

// 新增保存
const add = () => {
  data.form.userId = data.user.id
  request.post('/recharge/add', data.form).then(res => {
    if (res.code === '200') {
      load()
      loadAccount()//更新用户余额
      ElMessage.success('操作成功')
      data.formVisible = false
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 编辑保存
const update = () => {
  request.put('/recharge/update', data.form).then(res => {
    if (res.code === '200') {
      load()//更新列表
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
    request.delete('/recharge/delete/' + id).then(res => {
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
  data.username = null
  load()
}

</script>