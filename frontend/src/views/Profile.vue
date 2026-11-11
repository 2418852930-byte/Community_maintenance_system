<template>
  <div class="page-container">
    <!-- 顶部：角色感知的账号概览卡 -->
    <el-card shadow="hover" class="profile-card">
          <div class="cover" :style="{ background: roleGradient }"></div>

          <div class="avatar-wrap">
            <div class="avatar-ring" :style="{ borderColor: rolePrimary }"></div>
            <div class="avatar" :style="!userInfo.avatar ? { background: roleGradient } : {}">
              <!-- 有头像：显示图片；无头像：显示首字母占位 -->
              <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar__img" alt="头像" />
              <template v-else>{{ (userInfo.realName || 'U').charAt(0) }}</template>

              <!-- hover 遮罩：相机图标 + 更换提示 -->
              <div class="avatar__mask" @click="triggerAvatarUpload">
                <el-icon :size="20"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>

            <!-- 隐藏的 file input -->
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/png,image/jpeg,image/jpg"
              style="display: none"
              @change="handleAvatarChange"
            />

            <el-tag
              class="status-badge"
              :type="userInfo.status === 1 ? 'success' : 'danger'"
              effect="dark"
              size="small"
            >
              {{ userInfo.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>

          <div class="identity">
            <div class="name">{{ userInfo.realName || '-' }}</div>
            <div class="role-row">
              <span class="role-pill" :style="{ background: roleTintBg, color: rolePrimary }">
                <el-icon :size="12"><component :is="roleIcon" /></el-icon>
                {{ userInfo.roleName || '-' }}
              </span>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">登录账号</span>
              <span class="info-value">{{ userInfo.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userInfo.phone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">最近登录</span>
              <span class="info-value">{{ userInfo.lastLoginTime || '从未登录' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ userInfo.createTime || '-' }}</span>
            </div>

            <!-- 居民专属：楼栋房号 -->
            <template v-if="userStore.isResident">
              <div class="info-item info-item--full">
                <span class="info-label">居住地址</span>
                <span class="info-value">{{ userInfo.address || '未填写' }}</span>
              </div>
            </template>

            <!-- 维修人员专属：工种 / 工作状态 / 技能 -->
            <template v-if="userStore.isRepair">
              <div class="info-item">
                <span class="info-label">维修工种</span>
                <span class="info-value">{{ userInfo.workerType || '未选择' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">工作状态</span>
                <span class="info-value">{{ workStatusLabel }}</span>
              </div>
              <div
                v-if="userInfo.skillDesc"
                class="info-item info-item--full"
              >
                <span class="info-label">技能描述</span>
                <span class="info-value info-value--multi">{{ userInfo.skillDesc }}</span>
              </div>
            </template>
          </div>
    </el-card>

    <!-- 下方：资料维护 + 密码修改 -->
    <el-card shadow="never" class="edit-card">
          <el-tabs v-model="activeTab" class="profile-tabs">
            <!-- ============ 基本资料 ============ -->
            <el-tab-pane label="基本资料" name="profile">
              <!-- 只读信息区：账号本身不可修改 -->
              <div class="form-section form-section--readonly">
                <div class="section-title">
                  <el-icon :size="14"><Lock /></el-icon>
                  账号信息（不可修改）
                </div>
                <el-form :model="profileForm" label-width="100px">
                  <el-form-item label="登录账号">
                    <el-input :model-value="userInfo.username" disabled />
                  </el-form-item>
                  <el-form-item label="手机号">
                    <el-input :model-value="userInfo.phone" disabled />
                  </el-form-item>
                  <el-form-item label="角色">
                    <el-input :model-value="userInfo.roleName" disabled />
                  </el-form-item>
                </el-form>
              </div>

              <!-- 可编辑信息区 -->
              <div class="form-section">
                <div class="section-title">
                  <el-icon :size="14"><EditPen /></el-icon>
                  可修改资料
                </div>
                <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px">
                  <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" clearable />
                  </el-form-item>

                  <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="profileForm.gender">
                      <el-radio :value="0">未知</el-radio>
                      <el-radio :value="1">男</el-radio>
                      <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                  </el-form-item>

                  <el-form-item label="电子邮箱" prop="email">
                    <el-input v-model="profileForm.email" placeholder="选填，如 name@example.com" clearable />
                  </el-form-item>

                  <!-- 居民专属 -->
                  <el-form-item v-if="userStore.isResident" label="楼栋房号" prop="address">
                    <el-input v-model="profileForm.address" placeholder="如：幸福社区 3 栋 2 单元 502" clearable />
                  </el-form-item>

                  <!-- 维修人员专属 -->
                  <template v-if="userStore.isRepair">
                    <el-form-item label="维修工种" prop="workerType">
                      <el-select v-model="profileForm.workerType" placeholder="请选择工种" clearable style="width: 100%">
                        <el-option v-for="item in workerTypes" :key="item" :label="item" :value="item" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="技能描述" prop="skillDesc">
                      <el-input
                        v-model="profileForm.skillDesc"
                        type="textarea"
                        :rows="3"
                        maxlength="300"
                        show-word-limit
                        placeholder="选填，描述擅长的维修领域和经验年限"
                      />
                    </el-form-item>

                    <el-form-item label="工作状态" prop="workStatus">
                      <el-radio-group v-model="profileForm.workStatus">
                        <el-radio :value="0">休息</el-radio>
                        <el-radio :value="1">空闲</el-radio>
                        <el-radio :value="2">忙碌</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </template>

                  <el-form-item label-width="0">
                    <div class="form-actions">
                      <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">
                        保存修改
                      </el-button>
                      <el-button @click="resetProfileForm">重置</el-button>
                    </div>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- ============ 修改密码 ============ -->
            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="110px"
                class="pwd-form"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    show-password
                    placeholder="请输入当前密码"
                  />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <div class="pwd-input-wrap">
                    <el-input
                      v-model="pwdForm.newPassword"
                      type="password"
                      show-password
                      placeholder="6~20 位，须同时包含字母和数字"
                    />
                    <div v-if="pwdForm.newPassword" class="pwd-strength">
                      <div class="pwd-strength__bar">
                        <div
                          class="pwd-strength__fill"
                          :style="{ width: passwordStrength + '%', background: strengthColor }"
                        ></div>
                      </div>
                      <span class="pwd-strength__label" :style="{ color: strengthColor }">
                        {{ strengthLabel }}
                      </span>
                    </div>
                  </div>
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    show-password
                    placeholder="请再次输入新密码"
                  />
                </el-form-item>

                <el-form-item label-width="0">
                  <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">
                    确认修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Lock, EditPen, User, Tools, UserFilled, House
} from '@element-plus/icons-vue'
import { changePassword, updateProfile } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const activeTab = ref('profile')
const userInfo = computed(() => userStore.userInfo || {})

/** 与需求文档 5.8.5 节「工种」选项保持一致 */
const workerTypes = ['水电', '门窗', '公共照明', '给排水', '综合']

// ---------- 角色感知 ----------

/** 角色对应主色 + 渐变 + tint — 左侧头像卡和 pill 统一复用 */
const rolePrimary = computed(() => {
  switch (userStore.role) {
    case 'admin': return '#ef4444'
    case 'repair': return '#f59e0b'
    default: return '#10b981'
  }
})

const roleGradient = computed(() => {
  switch (userStore.role) {
    case 'admin': return 'linear-gradient(135deg, #f87171 0%, #ef4444 100%)'
    case 'repair': return 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)'
    default: return 'linear-gradient(135deg, #34d399 0%, #10b981 100%)'
  }
})

const roleTintBg = computed(() => {
  switch (userStore.role) {
    case 'admin': return 'rgba(239, 68, 68, 0.08)'
    case 'repair': return 'rgba(245, 158, 11, 0.08)'
    default: return 'rgba(16, 185, 129, 0.08)'
  }
})

const roleIcon = computed(() => {
  switch (userStore.role) {
    case 'admin': return UserFilled
    case 'repair': return Tools
    default: return House
  }
})

const roleTagType = computed(() => {
  switch (userStore.role) {
    case 'admin': return 'danger'
    case 'repair': return 'warning'
    default: return 'success'
  }
})

/** 工作状态中文名 */
const workStatusLabel = computed(() => {
  const map = { 0: '休息', 1: '空闲', 2: '忙碌' }
  return map[userInfo.value.workStatus] ?? '-'
})

// ---------- 密码强度 ----------

const passwordStrength = computed(() => {
  const pwd = pwdForm.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score += 25
  if (pwd.length >= 10) score += 15
  if (/[A-Za-z]/.test(pwd) && /\d/.test(pwd)) score += 30
  if (/[^A-Za-z0-9]/.test(pwd)) score += 20
  if (pwd.length >= 12 && /[^A-Za-z0-9]/.test(pwd)) score += 10
  return Math.min(score, 100)
})

const strengthLabel = computed(() => {
  if (passwordStrength.value <= 25) return '较弱'
  if (passwordStrength.value <= 50) return '一般'
  if (passwordStrength.value <= 75) return '较强'
  return '强'
})

const strengthColor = computed(() => {
  if (passwordStrength.value <= 25) return '#ef4444'
  if (passwordStrength.value <= 50) return '#f59e0b'
  if (passwordStrength.value <= 75) return '#3b82f6'
  return '#10b981'
})

// ------------------------- 基本资料 -------------------------
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = reactive({
  realName: '',
  gender: 0,
  email: '',
  address: '',
  workerType: '',
  skillDesc: '',
  workStatus: 1
})

const profileRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '真实姓名长度需为 2~10 个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/** 用当前用户信息回填表单 */
function resetProfileForm () {
  const info = userStore.userInfo || {}
  profileForm.realName = info.realName || ''
  profileForm.gender = info.gender === null || info.gender === undefined ? 0 : info.gender
  profileForm.email = info.email || ''
  profileForm.address = info.address || ''
  profileForm.workerType = info.workerType || ''
  profileForm.skillDesc = info.skillDesc || ''
  profileForm.workStatus = info.workStatus === null || info.workStatus === undefined ? 1 : info.workStatus
}

async function handleSaveProfile () {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileLoading.value = true
  try {
    // 按角色只提交允许修改的字段，避免触发后端 403
    const payload = {
      realName: profileForm.realName,
      gender: profileForm.gender,
      email: profileForm.email
    }
    if (userStore.isResident) {
      payload.address = profileForm.address
    }
    if (userStore.isRepair) {
      payload.workerType = profileForm.workerType
      payload.skillDesc = profileForm.skillDesc
      payload.workStatus = profileForm.workStatus
    }

    const res = await updateProfile(payload)
    userStore.setUserInfo(res.data)
    ElMessage.success('保存成功')
  } catch (e) {
    // 错误提示由响应拦截器处理
  } finally {
    profileLoading.value = false
  }
}

// ------------------------- 修改密码 -------------------------
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
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
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleChangePassword () {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    const res = await changePassword({ ...pwdForm })
    ElMessage.success(res.message || '密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdFormRef.value.resetFields()
  } catch (e) {
    // 错误提示由响应拦截器处理（如「原密码错误」）
  } finally {
    pwdLoading.value = false
  }
}

onMounted(async () => {
  try {
    await userStore.fetchCurrentUser()
  } catch (e) {
    // 401 已由拦截器统一处理
  }
  resetProfileForm()
})
</script>

<style scoped>
/* ==================== 左侧头像卡 ==================== */
.profile-card {
  overflow: visible !important;
  padding: 0 !important;
}

.profile-card :deep(.el-card__body) {
  padding: 0;
}

/* 顶部彩色覆盖层 */
.cover {
  height: 72px;
  border-radius: 16px 16px 0 0;
  opacity: 0.85;
  position: relative;
}

.cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 0%, rgba(255, 255, 255, 0.3) 100%);
}

/* 头像 —— 上浮到 cover 上方 */
.avatar-wrap {
  position: relative;
  margin-top: -34px;
  padding: 0 20px;
  display: flex;
  justify-content: center;
}

.avatar-ring {
  position: absolute;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.avatar {
  width: 66px;
  height: 66px;
  border-radius: 50%;
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: -0.5px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.status-badge {
  position: absolute;
  right: calc(50% - 44px);
  top: 44px;
}

/* 姓名 + 角色 pill */
.identity {
  padding: 16px 20px 8px;
  text-align: center;
}

.name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.role-row {
  margin-top: 8px;
}

.role-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

/* Key-value 信息网格 */
.info-grid {
  padding: 12px 20px 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  margin: 0 20px;
  padding-top: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-item--full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.info-value {
  font-size: 13px;
  color: #0f172a;
  font-weight: 500;
  line-height: 1.4;
}

.info-value--multi {
  font-size: 12px;
  color: #64748b;
  font-weight: 400;
}

/* ==================== 下方编辑卡 ==================== */
.edit-card {
  margin-top: 16px;
}

/* 表单分区 */
.form-section {
  margin-bottom: 24px;
}

.form-section--readonly {
  padding: 16px 20px;
  background: rgba(15, 23, 42, 0.02);
  border: 1px solid rgba(15, 23, 42, 0.04);
  border-radius: 12px;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.form-actions {
  display: flex;
  gap: 8px;
}

/* ==================== 密码强度指示器 ==================== */
.pwd-input-wrap {
  width: 100%;
}

.pwd-strength {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pwd-strength__bar {
  flex: 1;
  height: 4px;
  background: rgba(15, 23, 42, 0.08);
  border-radius: 9999px;
  overflow: hidden;
}

.pwd-strength__fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.2s ease, background 0.2s ease;
}

.pwd-strength__label {
  font-size: 12px;
  font-weight: 500;
  width: 40px;
  text-align: right;
}

/* ==================== 密码表单 ==================== */
.pwd-form {
  max-width: 480px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .status-badge {
    right: auto;
    left: calc(50% + 22px);
    top: 44px;
  }
}
</style>
