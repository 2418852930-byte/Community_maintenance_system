<template>
  <el-container class="layout">
    <!-- 左侧菜单：按当前登录角色动态过滤（角色菜单隔离） -->
    <el-aside width="240px" class="layout-aside">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="20"><Tools /></el-icon>
        </div>
        <div class="logo-text-group">
          <span class="logo-title">社区便民报修</span>
          <span class="logo-sub">维修管理系统</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#64748b"
        active-text-color="#0f172a"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-title">
          <span class="breadcrumb">工作台 /</span>
          <span class="current">{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <span class="role-tag" :class="'role-tag--' + userStore.role">{{ userStore.roleName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <span class="avatar">{{ (userStore.userInfo?.realName || 'U').charAt(0) }}</span>
              <span class="user-name">{{ userStore.userInfo?.realName || '未登录' }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="$route.fullPath" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 菜单来源：直接读取路由表中 MainLayout 的子路由，
 * 并用 meta.roles 过滤出当前角色可访问的项 —— 这样菜单与路由守卫共用同一份配置，不会出现「菜单可见但路由被拦」的情况。
 *
 * 模块二追加 meta.hidden 约定：hidden 的路由（如「提交报修」「工单详情」这类
 * 只能从列表页进入的页面）不渲染为菜单项，但权限仍由 meta.roles 照常约束。
 */
const menus = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === '/')
  const children = layoutRoute ? layoutRoute.children : []
  const role = userStore.role
  return children
    .filter((item) => item.meta && item.meta.title && item.meta.roles
      && !item.meta.hidden && item.meta.roles.includes(role))
    .map((item) => ({
      // 子路由 path 为相对路径，需要拼成绝对路径
      path: item.path.startsWith('/') ? item.path : `/${item.path}`,
      title: item.meta.title,
      icon: item.meta.icon || 'Menu'
    }))
})

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta.title || '首页')

/** 角色标签颜色：管理员红、维修人员橙、居民绿（仅作视觉区分） */
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

async function handleCommand (command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return // 用户取消
    }
    await userStore.doLogout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ---------- 侧边栏 —— 毛玻璃 ---------- */
.layout-aside {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-right: 1px solid rgba(15, 23, 42, 0.06);
  overflow-y: auto;
  transition: background 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.logo-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* el-menu 深度覆盖 —— 毛玻璃侧栏上的菜单项 */
:deep(.el-menu) {
  border-right: none;
  padding: 12px 10px;
}

:deep(.el-menu-item) {
  border-radius: 10px !important;
  margin: 2px 0 !important;
  height: 42px !important;
  line-height: 42px !important;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-menu-item:hover) {
  background: rgba(15, 23, 42, 0.03) !important;
}

:deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.08) !important;
  color: #0f172a !important;
  font-weight: 600;
  position: relative;
}

:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
}

:deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 16px;
}

/* ---------- 顶栏 —— 毛玻璃 ---------- */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px) saturate(1.8);
  -webkit-backdrop-filter: blur(16px) saturate(1.8);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.header-title .breadcrumb {
  color: #94a3b8;
}

.header-title .current {
  color: #0f172a;
  font-weight: 600;
  font-size: 15px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 角色标签 */
.role-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.role-tag--admin {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.role-tag--repair {
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.role-tag--resident {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #10b981;
}

/* 用户下拉 */
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 12px 4px 4px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  cursor: pointer;
  color: #0f172a;
  outline: none;
  transition: all 0.2s ease;
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(59, 130, 246, 0.2);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9999px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
}

.user-dropdown .arrow {
  font-size: 12px;
  color: #94a3b8;
}

/* ---------- 内容区 ---------- */
.layout-main {
  padding: 24px;
  overflow-y: auto;
}
</style>
