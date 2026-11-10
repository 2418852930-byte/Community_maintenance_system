<template>
  <div class="page-container">
    <div class="page-card">
      <el-result icon="info" :title="title" :sub-title="subTitle">
        <template #extra>
          <el-button type="primary" @click="router.push('/profile')">前往个人中心</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 占位页：用于「菜单隔离」演示。
 *
 * 本模块（登录注册与用户管理）只负责账号与角色体系，业务模块页面在后续阶段实现。
 * 保留菜单项是为了让三类角色的菜单差异可见、可演示。
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = computed(() => `${route.meta.title || '功能'}（待实现）`)
const subTitle = computed(() => {
  const stage = route.meta.stage || '后续阶段'
  return `当前登录角色：${userStore.roleName}。该页面属于「${stage}」，将在后续开发阶段按接口文档实现。`
})
</script>
