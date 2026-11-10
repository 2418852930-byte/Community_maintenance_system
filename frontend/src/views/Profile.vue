<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左侧：账号概览 -->
      <el-col :xs="24" :md="8">
        <div class="page-card">
          <div class="profile-head">
            <el-avatar :size="64" :src="userInfo.avatar || ''">
              {{ (userInfo.realName || '用户').charAt(0) }}
            </el-avatar>
            <div class="profile-name">{{ userInfo.realName || '-' }}</div>
            <el-tag :type="roleTagType" effect="dark" size="small">{{ userInfo.roleName || '-' }}</el-tag>
          </div>

          <el-descriptions :column="1" border size="small" style="margin-top: 16px">
            <el-descriptions-item label="用户ID">{{ userInfo.userId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登录账号">{{ userInfo.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ userInfo.lastLoginTime || '从未登录' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ userInfo.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>

      <!-- 右侧：资料维护与密码修改 -->
      <el-col :xs="24" :md="16">
        <div class="page-card">
          <el-tabs v-model="activeTab">
            <!-- U-07 修改本人资料 -->
            <el-tab-pane label="基本资料" name="profile">
              <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px">
                <el-form-item label="登录账号">
                  <el-input :model-value="userInfo.username" disabled />
                </el-form-item>

                <el-form-item label="手机号">
                  <el-input :model-value="userInfo.phone" disabled />
                  <div class="form-tip">手机号作为账号唯一标识不可自行修改，如需变更请联系管理员</div>
                </el-form-item>

                <el-form-item label="角色">
                  <el-input :model-value="userInfo.roleName" disabled />
                </el-form-item>

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
                  <el-input v-model="profileForm.email" placeholder="选填" clearable />
                </el-form-item>

                <!-- 居民专属：楼栋房号 -->
                <el-form-item v-if="userStore.isResident" label="楼栋房号" prop="address">
                  <el-input v-model="profileForm.address" placeholder="如：幸福社区 3 栋 2 单元 502" clearable />
                </el-form-item>

                <!-- 维修人员专属：工种 / 技能 / 工作状态 -->
                <template v-if="userStore.isRepair">
                  <el-form-item label="维修工种" prop="workerType">
                    <el-select v-model="profileForm.workerType" placeholder="请选择工种" clearable style="width: 100%">
                      <el-option v-for="item in workerTypes" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="技能描述" prop="skillDesc">
                    <el-input v-model="profileForm.skillDesc" type="textarea" :rows="3" placeholder="选填" />
                  </el-form-item>

                  <el-form-item label="工作状态" prop="workStatus">
                    <el-radio-group v-model="profileForm.workStatus">
                      <el-radio :value="0">休息</el-radio>
                      <el-radio :value="1">空闲</el-radio>
                      <el-radio :value="2">忙碌</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </template>

                <el-form-item>
                  <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">保存修改</el-button>
                  <el-button @click="resetProfileForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- U-06 修改本人密码 -->
            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 460px">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6~20 位，须同时包含字母和数字" />
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确认修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, updateProfile } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const activeTab = ref('profile')
const userInfo = computed(() => userStore.userInfo || {})

/** 与需求文档 5.8.5 节「工种」选项保持一致 */
const workerTypes = ['水电', '门窗', '公共照明', '给排水', '综合']

const roleTagType = computed(() => {
  switch (userStore.role) {
    case 'admin':
      return 'danger'
    case 'repair':
      return 'warning'
    default:
      return 'success'
  }
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
  // 进入页面时拉取一次最新资料（U-04），保证表单回显的是数据库最新数据
  try {
    await userStore.fetchCurrentUser()
  } catch (e) {
    // 401 已由拦截器统一处理
  }
  resetProfileForm()
})
</script>

<style scoped>
.profile-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 4px;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}
</style>
