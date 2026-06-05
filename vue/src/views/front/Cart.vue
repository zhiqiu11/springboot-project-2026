<template>
  <div class="front-container">
    <div class="card">
      <!-- 购物车列表 -->
      <div style="margin-bottom: 5px">
        <div style="margin-bottom: 10px">
          <el-button type="primary" @click="handleAdd">新增</el-button>
        </div>
        <el-table :data="data.tableData" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column label="商品图片">
            <template #default="scope">
              <img :src="scope.row.goodsImg"
                   fit="fill"
                   alt="商品图片"
                   style="width: 50px;"/>
            </template>
          </el-table-column>
          <el-table-column label="商品名称" prop="goodsName"></el-table-column>
          <el-table-column label="商品单价">
            <template #default="scope">
              <b style="color: red;">{{ scope.row.goodsPrice }} 元</b>
            </template>
          </el-table-column>
          <el-table-column label="商品数量">
            <template #default="scope">
              <el-input-number @click="changeNum(scope.row)" v-model="scope.row.num" type="number" :min="1" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="160">
            <template #default="scope">
              <el-button type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <!-- 第二行：总价格和下单按钮 -->
      <div style="text-align: right; margin-top: 20px; font-size: 20px">总价格：
        <b style="color: red; display: inline-block; min-width: 60px; text-align: left">{{ data.total }} 元</b>
        <div style="margin-top: 10px;padding: 20px"><el-button :disabled="data.total === 0" @click="handleAddOrder" type="danger">立即下单</el-button></div>
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

  </div>
</template>

<script setup>
import request from "@/utils/request";
import {reactive, ref} from "vue";
import {ElMessageBox, ElMessage} from "element-plus";

const formRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  total: 0,
  formVisible: false,
  form: {},
  tableData: [],
  name: null,
  selectedRows: [],
  rules: {
    deliverType: [{ required: true, message: '请选择配送类型', trigger: 'change' }],
    address: [{ required: true, message: '请输入收获地址', trigger: 'blur' }]
  }
})

// 点击购买时的弹窗
const handleAddOrder = () => {
  data.formVisible = true
  data.form = {}
}

// 下单
const addOrder = () => {
  // 检查是否有选中的商品
  if (!data.selectedRows?.length) {
    ElMessage.error('请选择商品')
    return
  }
  formRef.value.validate(valid => {
    if (valid) {
      // 检查配送类型是否选择
      data.form.userId = data.user.id
      data.form.cartList = [{goodsId: data.id, num: data.num}]
      request.post('/orders/add', data.form).then(res => {
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

// 商品数量相关改变
const changeNum = (row) => {
  calculateTotal()
  data.form = row
  update()
}

// 计算总价格
const calculateTotal = () => {
  data.total = 0
  data.selectedRows.forEach(row => {
    data.total += row.goodsPrice * row.num
  })
  if (data.total > 0) {
    data.total = Math.round(data.total * 100) / 100//这里是前端js的计算精度问题，需要先乘以100，再除以100才能得到正确的金额
  }
}

// 选中行变化
const handleSelectionChange = (rows) => {
  data.selectedRows = rows
  calculateTotal()
}

// 分页查询
const load = () => {
  request.get('/cart/selectAll', {
    params: {
      userId: data.user.id,
    }
  }).then(res => {
    data.tableData = res.data
  })
}
load()

// 新增
const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}

// 编辑
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

// 新增保存
const add = () => {
  request.post('/cart/add', data.form).then(res => {
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
  request.put('/cart/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
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
  ElMessageBox.confirm('您确定删除吗?', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/cart/delete/' + id).then(res => {
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
  data.name = null
  load()
}

</script>