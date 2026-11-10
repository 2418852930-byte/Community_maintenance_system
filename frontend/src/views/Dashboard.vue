<template>
  <div class="page-container">
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-inner">
        <el-avatar :size="56">{{ (userInfo.realName || '用').charAt(0) }}</el-avatar>
        <div class="welcome-text">
          <div class="welcome-title">
            {{ greeting }}，{{ userInfo.realName || '用户' }}
          </div>
          <div class="welcome-sub">
            当前角色：{{ userInfo.roleName }} ｜ 手机号：{{ userInfo.phone }}
            ｜ 最近登录：{{ userInfo.lastLoginTime || '本次为首次登录' }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 账号数据概览：复用 U-08 用户分页列表的 total 字段做计数 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col v-for="item in stats" :key="item.role" :xs="24" :sm="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-unit">个账号</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span>下一步操作</span>
          </template>
          <el-space wrap>
            <el-button type="primary" @click="router.push('/users')">用户管理</el-button>
            <el-button @click="router.push('/profile')">个人中心</el-button>
          </el-space>
          <el-alert
            title="说明：报修统计分析模块（看板图表、趋势与分布统计）属于阶段七，将在后续阶段基于 S-01 ~ S-09 接口实现。"
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 16px"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageUsers } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = ref([
  { role: 'resident', label: '居民', value: 0, color: '#67c23a' },
  { role: 'repair', label: '维修人员', value: 0, color: '#e6a23c' },
  { role: 'admin', label: '系统管理员', value: 0, color: '#f56c6c' }
])

/**
 * 复用 U-08 用户分页列表接口，按角色分别取 total 作为账号数量。
 * pageSize 传 1 即可，只需 total 字段，避免拉取多余数据。
 */
async function loadStats () {
  for (const item of stats.value) {
    try {
      const res = await pageUsers({ pageNum: 1, pageSize: 1, role: item.role })
      item.value = res.data.total || 0
    } catch (e) {
      item.value = 0
    }
  }
}

onMounted(loadStats)
</script>

<style scoped>
.welcome-card {
  border-radius: 6px;
}

.welcome-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.welcome-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.stat-card {
  border-radius: 6px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-value {
  margin: 8px 0 2px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.stat-unit {
  font-size: 12px;
  color: #909399;
}
</style>
