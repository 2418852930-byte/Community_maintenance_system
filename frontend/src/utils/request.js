import axios from 'axios'
import { ElMessage } from 'element-plus'

/** localStorage 中保存 token 与用户信息的键名，供 store 与拦截器共用 */
export const TOKEN_KEY = 'cr_token'
export const USER_KEY = 'cr_user_info'

/**
 * axios 实例
 * - baseURL 为 /api，开发环境由 vite.config.js 代理到后端 8080
 * - 超时 10 秒
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 请求拦截器：自动在请求头携带 JWT
 * 对应接口文档 2.7 节，格式为 Authorization: Bearer <token>
 */
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 防止多个并发请求同时触发 401 跳转 */
let redirecting = false

/**
 * 清除本地登录状态并跳转登录页
 */
function toLogin () {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (!redirecting) {
    redirecting = true
    // 使用整页跳转而非路由跳转，可同时重置 Pinia 状态，且避免与 router 产生循环依赖
    window.location.href = '/login'
  }
}

/**
 * 响应拦截器：统一处理接口文档 2.4 节的状态码
 * 业务成功（code = 200）时直接把后端返回的 { code, message, data } 交给调用方
 */
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端异常由 GlobalExceptionHandler 统一返回 Result 结构
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    const response = error.response
    if (!response) {
      ElMessage.error('网络异常，请检查后端服务是否已启动')
      return Promise.reject(error)
    }
    const { status, data } = response
    const message = (data && data.message) || '请求失败'

    switch (status) {
      case 400:
        // 参数校验失败时，后端会在 data 中返回「字段 -> 提示」的明细
        if (data && data.data && typeof data.data === 'object') {
          const first = Object.values(data.data)[0]
          ElMessage.error(first || message)
        } else {
          ElMessage.error(message)
        }
        break
      case 401:
        // 未登录 / 令牌过期 / 账号已被禁用
        ElMessage.error(message || '登录已失效，请重新登录')
        toLogin()
        break
      case 403:
        ElMessage.error(message || '无操作权限')
        break
      case 404:
        ElMessage.error(message || '资源不存在')
        break
      case 409:
        ElMessage.error(message || '操作冲突')
        break
      case 500:
        ElMessage.error(message || '服务器内部错误')
        break
      default:
        ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service
