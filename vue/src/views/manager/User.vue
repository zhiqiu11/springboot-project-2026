<template>
  <div style="display: flex;gap: 10px; width: 100%">
    <div class ="card" style ="margin-bottom: 5px; flex: 1">
      <el-input style ="width: 300px; margin-right: 10px" v-model="data.name" placeholder="请输入名称查询" :prefix-icon="Search"/>
      <el-input style ="width: 300px; margin-right: 10px" v-model="data.id" placeholder="请输入ID查询" :prefix-icon="Search"/>
      <el-button @click="load" type="primary">查询</el-button>
      <el-button @click="reset" type="info">重置</el-button>
    </div>
  </div>

  <div class="card" style="margin-bottom: 5px">
    <div style="margin-bottom: 10px">
      <el-button @click="handleAdd" type="primary">新增</el-button>
    </div>
    <div>
      <el-table :data="data.tableData" stripe style="width: 100%">
        <el-table-column prop="username" label="账号"/>
        <el-table-column prop="name" label="姓名"/>
        <el-table-column prop="avatar" label="头像">
          <template #default="scope">
            <el-image v-if="scope.row.avatar"
                      style="width: 50px;height: 50px; display: block; border-radius: 50%;"
                      :src="scope.row.avatar"
                      :preview-src-list="[scope.row.avatar]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="account" label="账户余额" />
        <el-table-column label="操作" width="175px" fixed="right">
          <template #default="scope">
            <el-button type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" @click="deleteRow(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
  <div class="card" >
    <el-pagination v-model:current-page="data.pageNum" v-model:page-size="data.pageSize"
                   @current-change="load" background layout="total, prev, pager, next" :total="data.total" />
  </div>

  <el-dialog v-model="data.formVisible" title="用户信息" width="30%" destroy-on-close>
    <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="80px" style="padding-right: 30px">
      <el-form-item label="账号" prop="username">
        <el-input :disabled="data.form.id !== undefined" v-model="data.form.username" placeholder="请输入账号" autocomplete="off" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="data.form.name" placeholder="请输入姓名" autocomplete="off" />
      </el-form-item>
      <el-form-item label="头像" prop="avatar">
        <el-upload
            :action="baseUrl + '/files/upload'"
            list-type="picture"
            :on-success="handleFileUpload"
            :file-list="fileList"
        >
          <el-button type="primary">上传头像</el-button>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </div>
    </template>
  </el-dialog>

</template>

<script setup>
import {reactive, ref} from "vue";
import {Search} from "@element-plus/icons-vue";
import request from "@/utils/request";
import {ElMessage, ElMessageBox} from "element-plus";

const fileList = ref([])
const baseUrl = import.meta.env.VITE_BASE_URL
const formRef = ref()
const data = reactive({
  id: null,
  name: null,
  tableData: [],
  total:0,
  pageNum:1,
  pageSize:5,
  formVisible: false,
  form:{},
  rules:{
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 5, message: '长度应为 3 到 5', trigger: 'blur' }
    ],
    name:[
      { required: true, message: '请输入账号', trigger: 'blur' }
    ]
  }
})

//分页查询的函数
const load = () =>{
  request.get('user/selectPage', {
    params:{
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name:data.name,
      id: data.id
    }
  }).then(res =>{

    if (res.code ==='200'){
      data.tableData = res.data?.list
      data.total = res.data?.total
    } else{
      ElMessage.error(res.msg)
    }
  })
}
load()
//重置函数
const reset = ()=>{
  data.name = null
  data.id = null
  load()
}

const deleteRow = (id) =>{
  ElMessageBox.confirm('是否确认删除？', '是否删除', {type:'warning'}).then(res =>{
    request.delete('/user/delete/' + id).then(res =>{
      if (res.code ==='200'){
        ElMessage.success("操作成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {})
}

const handleAdd = () =>{
  data.form = {}
  data.formVisible = true
  fileList.value = [] // 清空上传列表
}

const handleEdit = (row) =>{
  data.form = JSON.parse(JSON.stringify(row))
  if (row.avatar) {
    fileList.value = [{ url: row.avatar, name: '头像' }]
  } else {
    fileList.value = []
  }
  data.formVisible = true
}

function add() {
  request.post('/user/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success("操作成功")
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

function update() {
  request.put('/user/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success("操作成功")
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) { //表单校验通过语句
      data.form.id?update():add()
    }
  })
}

//表单头像上传组件的回调函数
const handleFileUpload = (response) =>{
  console.log(response)
  if (response.code === '200') {
    data.form.avatar = response.data
  } else {
    ElMessage.error('头像上传失败')
    fileList.value = []
    data.form.avatar = null
  }
}
</script>
