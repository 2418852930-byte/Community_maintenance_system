<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 顶部品牌区 -->
      <div class="brand">
        <div class="brand-icon">
          <el-icon :size="22"><Tools /></el-icon>
        </div>
        <h2 class="auth-title">社区便民报修服务管理系统</h2>
        <p class="auth-subtitle">登录以继续使用</p>
      </div>

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
        <el-link type="primary" @click="router.push('/register')">注册新账号</el-link>
        <span class="divider-dot">·</span>
        <el-link type="info" @click="router.push('/forgot-password')">忘记密码？</el-link>
      </div>

      <!-- 演示账号提示 -->
      <div class="demo-hint">
        <el-icon :size="14"><InfoFilled /></el-icon>
        <span>演示：admin / 123456 · repair01 / 123456 · resident01 / 123456</span>
      </div>
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

<style scoped>
/* 顶部品牌区 */
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 4px;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: #fff;
  margin-bottom: 16px;
  box-shadow:
    0 2px 4px rgba(59, 130, 246, 0.2),
    0 8px 24px rgba(139, 92, 246, 0.2);
}

.auth-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  text-align: center;
  letter-spacing: 0.3px;
}

.auth-subtitle {
  margin: 0 0 24px;
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

/* 底部链接 */
.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.divider-dot {
  color: #cbd5e1;
  font-size: 14px;
}

/* 演示账号 pill */
.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  padding: 10px 16px;
  background: rgba(59, 130, 246, 0.06);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 12px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
</style>
