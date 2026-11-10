import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/utils/request'

/**
 * 登录用户状态（Pinia）。
 *
 * 职责：
 *  1. 保存 token 与用户信息，并持久化到 localStorage（刷新页面不丢登录态）；
 *  2. 提供角色相关的 getter 与「角色 -> 首页」映射，供路由守卫与菜单渲染使用。
 *
 * 注意：前端角色判断只用于「菜单展示与路由跳转」，
 * 真正的权限控制由后端 JwtInterceptor 强制校验（NFR-S-03）。
 */

/** 角色中文名，与后端 RoleEnum 保持一致 */
export const ROLE_NAMES = {
  resident: '居民',
  repair: '维修人员',
  admin: '系统管理员'
}

/**
 * 各角色登录后的默认首页（BR-8-10）：
 * 居民 -> 我的报修，维修人员 -> 我的工单，系统管理员 -> 统计概览看板
 */
export const ROLE_HOME = {
  resident: '/my-orders',
  repair: '/my-tasks',
  admin: '/dashboard'
}

function readUserFromStorage () {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readUserFromStorage()
  }),

  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.token,
    /** 当前角色编码：resident / repair / admin */
    role: (state) => (state.userInfo ? state.userInfo.role : ''),
    /** 当前角色中文名 */
    roleName: (state) => (state.userInfo ? state.userInfo.roleName : ''),
    /** 是否系统管理员 */
    isAdmin: (state) => state.userInfo && state.userInfo.role === 'admin',
    /** 是否居民 */
    isResident: (state) => state.userInfo && state.userInfo.role === 'resident',
    /** 是否维修人员 */
    isRepair: (state) => state.userInfo && state.userInfo.role === 'repair'
  },

  actions: {
    /** 保存用户信息（同时持久化） */
    setUserInfo (userInfo) {
      this.userInfo = userInfo
      if (userInfo) {
        localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
      } else {
        localStorage.removeItem(USER_KEY)
      }
    },

    /** 保存 token */
    setToken (token) {
      this.token = token || ''
      if (token) {
        localStorage.setItem(TOKEN_KEY, token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
    },

    /**
     * 登录：调用 U-02 接口，成功后保存 token 与用户信息
     * @param {{account: string, password: string}} payload
     * @returns {Promise<Object>} userInfo
     */
    async doLogin (payload) {
      const res = await loginApi(payload)
      const data = res.data || {}
      this.setToken(data.token)
      this.setUserInfo(data.userInfo)
      return data.userInfo
    },

    /**
     * 拉取当前登录用户信息（U-04），用于刷新页面后恢复用户信息
     */
    async fetchCurrentUser () {
      const res = await getCurrentUser()
      this.setUserInfo(res.data)
      return res.data
    },

    /**
     * 退出登录：调用 U-03 使服务端令牌失效，并清除本地状态
     */
    async doLogout () {
      try {
        if (this.token) {
          await logoutApi()
        }
      } catch (e) {
        // 即使服务端调用失败，也必须清除本地登录态，保证用户能正常退出
      } finally {
        this.setToken('')
        this.setUserInfo(null)
      }
    },

    /** 清除本地登录态（token 失效时使用） */
    clear () {
      this.setToken('')
      this.setUserInfo(null)
    }
  }
})
