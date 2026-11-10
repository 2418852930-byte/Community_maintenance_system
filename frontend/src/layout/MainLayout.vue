<template>
  <el-container class="layout">
    <!-- 左侧菜单：按当前登录角色动态过滤（角色菜单隔离） -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span class="logo-text">社区便民报修</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.75)"
        active-text-color="#ffffff"
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
        <div class="header-title">{{ currentTitle }}</div>

        <div class="header-right">
          <el-tag :type="roleTagType" size="small" effect="dark">{{ userStore.roleName }}</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><UserFilled /></el-icon>
              <span class="user-name">{{ userStore.userInfo?.realName || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
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

.layout-aside {
  background-color: #001529;
  overflow-y: auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 16px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  white-space: nowrap;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
  outline: none;
}

.user-name {
  font-size: 14px;
}

.layout-main {
  padding: 16px;
  background-color: #f5f7fa;
  overflow-y: auto;
}

/* 让 el-menu 在深色侧栏上不出现白边 */
:deep(.el-menu) {
  border-right: none;
}
</style>
