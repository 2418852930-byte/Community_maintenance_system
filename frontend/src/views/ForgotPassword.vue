<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">找回密码</h2>
      <p class="auth-subtitle">校验用户名与注册预留手机号后可重置密码</p>

      <!-- U-05 忘记密码 / 自助重置密码 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入注册时的用户名" clearable />
        </el-form-item>

        <el-form-item label="预留手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入注册时的手机号" clearable maxlength="11" />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="6~20 位，须同时包含字母和数字" show-password />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            重 置 密 码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forgotPassword } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入注册预留手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/,
      message: '密码需为 6~20 位且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleSubmit () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await forgotPassword({ ...form, username: form.username.trim(), phone: form.phone.trim() })
    ElMessage.success(res.message || '密码重置成功')
    router.push('/login')
  } catch (e) {
    // 错误提示由响应拦截器处理（如「用户名与预留手机号不匹配」）
  } finally {
    loading.value = false
  }
}
</script>
