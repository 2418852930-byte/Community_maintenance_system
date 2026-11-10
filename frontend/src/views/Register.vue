<template>
  <div class="auth-page">
    <div class="auth-card" style="width: 520px">
      <h2 class="auth-title">居民账号注册</h2>
      <p class="auth-subtitle">维修人员与管理员账号由管理员在后台创建</p>

      <!-- U-01 居民账号注册，角色固定为「居民」 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4~20 位字母、数字或下划线" clearable />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号，作为账号唯一标识" clearable maxlength="11" />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>

        <el-form-item label="楼栋房号" prop="address">
          <el-input v-model="form.address" placeholder="如：幸福社区 3 栋 2 单元 502，将作为报修位置默认值" clearable />
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6~20 位，须同时包含字母和数字" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <span style="color: #909399; font-size: 13px">已有账号？</span>
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  phone: '',
  realName: '',
  address: '',
  email: '',
  password: '',
  confirmPassword: ''
})

/** 与后端 BR-8-01 ~ BR-8-05 保持一致的校验规则，前端先提示，后端仍会二次校验 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '用户名需为 4~20 位字母、数字或下划线', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '真实姓名长度需为 2~10 个字符', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入楼栋房号', trigger: 'blur' },
    { min: 2, max: 50, message: '楼栋房号长度需为 2~50 字', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/,
      message: '密码需为 6~20 位且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      // 两次密码一致性（BR-8-04），前端即时提示
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleRegister () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({ ...form, username: form.username.trim(), phone: form.phone.trim() })
    ElMessage.success(res.message || '注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误提示已由响应拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
