<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">社区便民报修服务管理系统</h2>
      <p class="auth-subtitle">登录以继续使用</p>

      <!-- U-02 账号登录，支持用户名或手机号 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入用户名或手机号" clearable :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <el-link type="primary" @click="router.push('/register')">居民注册</el-link>
        <el-divider direction="vertical" />
        <el-link type="info" @click="router.push('/forgot-password')">忘记密码？</el-link>
      </div>

      <el-alert
        title="演示账号：admin / 123456（管理员）、repair01 / 123456（维修人员）、resident01 / 123456（居民）"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore, ROLE_HOME } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const userInfo = await userStore.doLogin({
      account: form.account.trim(),
      password: form.password
    })
    ElMessage.success(`欢迎回来，${userInfo.realName}`)
    // BR-8-10：按角色跳转对应首页；若从被拦截页面跳转而来则回到原页面
    const redirect = route.query.redirect
    router.push(redirect || ROLE_HOME[userInfo.role] || '/dashboard')
  } catch (e) {
    // 错误提示已由 axios 响应拦截器统一处理（如「用户名或密码错误」「账号已被禁用」）
  } finally {
    loading.value = false
  }
}
</script>
