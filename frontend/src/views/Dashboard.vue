<template>
  <div class="page-container">
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-inner">
        <div class="welcome-avatar">{{ (userInfo.realName || 'U').charAt(0) }}</div>
        <div class="welcome-text">
          <div class="welcome-title">
            {{ greeting }}，{{ userInfo.realName || '用户' }}
          </div>
          <div class="welcome-sub">
            当前角色：{{ userInfo.roleName }} · 手机号：{{ userInfo.phone }}
            · 最近登录：{{ userInfo.lastLoginTime || '本次为首次登录' }}
          </div>
        </div>
        <div class="welcome-side">
          <span class="side-label">今日日期</span>
          <span class="side-value">{{ todayLabel }}</span>
        </div>
      </div>
    </el-card>

    <!-- 账号数据概览 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in stats" :key="item.role" :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-top">
            <div class="stat-icon" :style="{ background: item.tintBg }">
              <el-icon :size="20" :style="{ color: item.color }">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="stat-spark"></div>
          </div>
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作：按角色动态展示 -->
    <el-card shadow="never" class="actions-card">
      <template #header>
        <span class="card-header-title">快速操作</span>
      </template>
      <div class="action-grid">
        <button
          v-for="btn in quickActions"
          :key="btn.path"
          class="action-btn"
          @click="router.push(btn.path)"
        >
          <span class="action-icon" :style="{ background: btn.tintBg, color: btn.color }">
            <el-icon><component :is="btn.icon" /></el-icon>
          </span>
          <div class="action-text">
            <span class="action-label">{{ btn.label }}</span>
            <span class="action-desc">{{ btn.desc }}</span>
          </div>
        </button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, UserFilled, Tools, DataBoard, User as UserIcon,
  DocumentChecked, Promotion, TrendCharts, CircleCheck,
  ChatDotSquare, Files
} from '@element-plus/icons-vue'
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

const todayLabel = computed(() => {
  const d = new Date()
  const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth() + 1}月${d.getDate()}日 · ${weekMap[d.getDay()]}`
})

const stats = ref([
  { role: 'resident', label: '居民用户', value: 0, color: '#10b981', tintBg: 'rgba(16,185,129,0.08)', icon: User },
  { role: 'repair', label: '维修人员', value: 0, color: '#f59e0b', tintBg: 'rgba(245,158,11,0.08)', icon: Tools },
  { role: 'admin', label: '系统管理员', value: 0, color: '#ef4444', tintBg: 'rgba(239,68,68,0.08)', icon: UserFilled }
])

/** 快速操作：按角色动态展示 */
const quickActions = computed(() => {
  const role = userStore.role
  // 共享操作
  const common = [
    { path: '/profile', label: '个人中心', desc: '查看 / 修改个人资料', icon: UserIcon, color: '#10b981', tintBg: 'rgba(16,185,129,0.08)' }
  ]

  if (role === 'admin') {
    return [
      ...common,
      { path: '/orders/audit', label: '工单审核', desc: '审核居民提交的报修', icon: DocumentChecked, color: '#3b82f6', tintBg: 'rgba(59,130,246,0.08)' },
      { path: '/orders/dispatch', label: '派单调度', desc: '给维修人员派单', icon: Promotion, color: '#8b5cf6', tintBg: 'rgba(139,92,246,0.08)' },
      { path: '/order-verify', label: '工单验收', desc: '验收超时代验收', icon: CircleCheck, color: '#06b6d4', tintBg: 'rgba(6,182,212,0.08)' },
      { path: '/statistics', label: '统计分析', desc: '报修数据看板', icon: TrendCharts, color: '#f59e0b', tintBg: 'rgba(245,158,11,0.08)' },
      { path: '/users', label: '用户管理', desc: '管理三类账号', icon: UserFilled, color: '#ef4444', tintBg: 'rgba(239,68,68,0.08)' },
      { path: '/categories', label: '报修类目', desc: '类目增删改查', icon: Files, color: '#ec4899', tintBg: 'rgba(236,72,153,0.08)' },
      { path: '/evaluations', label: '评价管理', desc: '回复服务评价', icon: ChatDotSquare, color: '#14b8a6', tintBg: 'rgba(20,184,166,0.08)' }
    ]
  }

  if (role === 'repair') {
    return [
      ...common,
      { path: '/order-hall', label: '工单大厅', desc: '抢单 / 接单 / 拒单', icon: Promotion, color: '#3b82f6', tintBg: 'rgba(59,130,246,0.08)' },
      { path: '/my-tasks', label: '我的工单', desc: '维修进度管理', icon: Tools, color: '#f59e0b', tintBg: 'rgba(245,158,11,0.08)' },
      { path: '/my-evaluations', label: '收到的评价', desc: '查看用户评分', icon: ChatDotSquare, color: '#14b8a6', tintBg: 'rgba(20,184,166,0.08)' }
    ]
  }

  // resident
  return [
    ...common,
    { path: '/orders/submit', label: '提交报修', desc: '新建报修工单', icon: DocumentChecked, color: '#3b82f6', tintBg: 'rgba(59,130,246,0.08)' },
    { path: '/my-orders', label: '我的报修', desc: '查看报修记录', icon: Promotion, color: '#8b5cf6', tintBg: 'rgba(139,92,246,0.08)' },
    { path: '/order-verify', label: '工单验收', desc: '验收维修结果', icon: CircleCheck, color: '#06b6d4', tintBg: 'rgba(6,182,212,0.08)' },
    { path: '/my-evaluations', label: '服务评价', desc: '评价维修服务', icon: ChatDotSquare, color: '#14b8a6', tintBg: 'rgba(20,184,166,0.08)' }
  ]
})

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
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  box-shadow:
    0 4px 12px rgba(59, 130, 246, 0.2),
    0 1px 3px rgba(15, 23, 42, 0.08);
  flex-shrink: 0;
}

.welcome-text {
  flex: 1;
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
  line-height: 1.5;
}

.welcome-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding-left: 20px;
  border-left: 1px solid rgba(15, 23, 42, 0.08);
}

.side-label {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.side-value {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

/* ---------- 统计卡片 ---------- */
.stat-row {
  margin-top: 16px;
}

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
}

.stat-spark {
  width: 48px;
  height: 24px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    currentColor 20%,
    currentColor 40%,
    transparent 55%,
    currentColor 70%,
    currentColor 85%,
    transparent 100%
  );
  opacity: 0.12;
  mask-image: linear-gradient(90deg, transparent, #000 20%, #000 80%, transparent);
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 20%, #000 80%, transparent);
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
.actions-card {
  margin-top: 16px;
}

.card-header-title {
  font-weight: 600;
  color: #0f172a;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 14px;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(59, 130, 246, 0.25);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(59, 130, 246, 0.08),
    0 1px 3px rgba(15, 23, 42, 0.04);
}

.action-btn:active {
  transform: translateY(0);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.action-btn:hover .action-icon {
  transform: scale(1.06);
}

.action-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.action-label {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.3;
}

.action-desc {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .welcome-inner {
    flex-wrap: wrap;
  }

  .welcome-side {
    width: 100%;
    padding-left: 0;
    padding-top: 12px;
    margin-top: 12px;
    border-left: none;
    border-top: 1px solid rgba(15, 23, 42, 0.08);
    align-items: flex-start;
  }
}
</style>
