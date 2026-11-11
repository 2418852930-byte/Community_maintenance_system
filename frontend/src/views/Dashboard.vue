<template>
  <div class="page-container">
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-inner">
        <div class="welcome-avatar">{{ (userInfo.realName || 'U').charAt(0) }}</div>
        <div class="welcome-text">
          <div class="welcome-title">
            {{ greeting }}，{{ userInfo.realName || '用户' }} 👋
          </div>
          <div class="welcome-sub">
            当前角色：{{ userInfo.roleName }} · 手机号：{{ userInfo.phone }}
            · 最近登录：{{ userInfo.lastLoginTime || '本次为首次登录' }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 账号数据概览 -->
    <el-row :gutter="16" style="margin-top: 20px">
      <el-col v-for="item in stats" :key="item.role" :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-top">
            <div class="stat-icon" :style="{ background: item.tintBg }">
              <el-icon :size="20" :style="{ color: item.color }">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="stat-trend">—</div>
          </div>
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span class="card-header-title">快速操作</span>
          </template>
          <div class="action-grid">
            <button class="action-btn" @click="router.push('/users')">
              <span class="action-icon" style="background: rgba(59,130,246,0.08); color: #3b82f6">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span>用户管理</span>
            </button>
            <button class="action-btn" @click="router.push('/profile')">
              <span class="action-icon" style="background: rgba(16,185,129,0.08); color: #10b981">
                <el-icon><User /></el-icon>
              </span>
              <span>个人中心</span>
            </button>
          </div>
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
  { role: 'resident', label: '居民用户', value: 0, color: '#10b981', tintBg: 'rgba(16,185,129,0.08)', icon: 'User' },
  { role: 'repair', label: '维修人员', value: 0, color: '#f59e0b', tintBg: 'rgba(245,158,11,0.08)', icon: 'Tools' },
  { role: 'admin', label: '系统管理员', value: 0, color: '#ef4444', tintBg: 'rgba(239,68,68,0.08)', icon: 'UserFilled' }
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
/* ---------- 欢迎卡片 ---------- */
.welcome-card {
  border-radius: 20px !important;
}

.welcome-inner {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
  flex-shrink: 0;
}

.welcome-text {
  min-width: 0;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.welcome-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #94a3b8;
}

/* ---------- 统计卡片 ---------- */
.stat-card {
  border-radius: 16px !important;
  padding: 18px 20px 16px;
}

.stat-card :deep(.el-card__body) {
  padding: 0;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.stat-trend {
  font-size: 12px;
  color: #94a3b8;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

/* ---------- 快速操作 ---------- */
.card-header-title {
  font-weight: 600;
  color: #0f172a;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 12px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(59, 130, 246, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
  color: #0f172a;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
}
</style>
