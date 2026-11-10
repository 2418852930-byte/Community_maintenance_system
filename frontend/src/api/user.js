import request from '@/utils/request'

/**
 * 用户管理类接口（U-08 ~ U-14）
 * 对应后端 UserController（/api/users/**），仅系统管理员可访问
 */

/** U-08 用户分页列表，params: { pageNum, pageSize, role, keyword, status, workerType, workStatus, startTime, endTime } */
export function pageUsers (params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

/** U-09 用户详情 */
export function getUserDetail (userId) {
  return request({
    url: `/users/${userId}`,
    method: 'get'
  })
}

/** U-10 新增用户（仅支持 repair / admin 角色） */
export function createUser (data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

/** U-11 修改用户基本信息 */
export function updateUser (userId, data) {
  return request({
    url: `/users/${userId}`,
    method: 'put',
    data
  })
}

/** U-12 角色分配与变更 */
export function changeRole (userId, role) {
  return request({
    url: `/users/${userId}/role`,
    method: 'put',
    data: { role }
  })
}

/** U-13 账号启用 / 禁用 */
export function changeStatus (userId, status) {
  return request({
    url: `/users/${userId}/status`,
    method: 'put',
    data: { status }
  })
}

/** U-14 重置用户密码 */
export function resetPassword (userId, data) {
  return request({
    url: `/users/${userId}/reset-password`,
    method: 'put',
    data
  })
}
