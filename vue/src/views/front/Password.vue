<template>
  <div class="front-container" style="width: 40%">
    <div class="card" style="padding: 20px">
      <div style="font-size: 20px; margin-bottom: 40px; text-align: center">修改密码</div>
      <el-form ref="formRef" :model="data.user" :rules="data.rules" label-width="80px" style="padding-right: 30px">
        <el-form-item label="原密码" prop="password">
          <el-input v-model="data.user.password" show-password autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="data.user.newPassword" show-password autocomplete="off" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="data.user.confirmPassword" show-password autocomplete="off" />
        </el-form-item>
        <div style="text-align: center">
          <el-button type="primary" size="large" @click="updatePassword">保存</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request";
import { ElMessage } from "element-plus";
import router from "@/router";

const formRef = ref();
const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
  rules: {
    password: [
      { required: true, message: '请输入原密码', trigger: 'blur' }
    ],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' }
    ]
  }
});

function updatePassword() {
  if (data.user.newPassword !== data.user.confirmPassword) {
    ElMessage.error("输入的密码不一致，请重新输入！");
    return;
  }
  request.put('/updatePassword', data.user).then(res => {
    if (res.code === '200') {
      ElMessage.success("更新成功");
      logout();
    } else {
      ElMessage.error(res.msg);
    }
  });
}

const logout = () => {
  router.push('/login');
  ElMessage.success('请重新登录');
  localStorage.removeItem('system-user');
  localStorage.removeItem(`token_${data.user.role}_${data.user.id}`);
  sessionStorage.removeItem('currentRole');
  sessionStorage.removeItem('currentId');
};
</script>