import request from '@/utils/request'

/**
 * 认证类接口（U-01 ~ U-07）
 * 对应后端 AuthController（/api/auth/**）
 *
 * 说明：request 实例的 baseURL 已是 /api，因此此处无需重复书写 /api 前缀。
 */

/** U-01 居民账号注册（免鉴权） */
export function register (data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/** U-02 账号登录（免鉴权），返回 { token, tokenType, expiresIn, userInfo } */
export function login (data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/** U-03 退出登录 */
export function logout () {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** U-04 获取当前登录用户信息 */
export function getCurrentUser () {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

/** U-05 忘记密码 / 自助重置密码（免鉴权） */
export function forgotPassword (data) {
  return request({
    url: '/auth/forgot-password',
    method: 'post',
    data
  })
}

/** U-06 修改本人密码 */
export function changePassword (data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}

/** U-07 修改本人资料 */
export function updateProfile (data) {
  return request({
    url: '/auth/me',
    method: 'put',
    data
  })
}
