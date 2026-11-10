import request from '@/utils/request'

/**
 * 报修工单提交模块接口（O-01 ~ O-06）
 * 对应后端 RepairOrderController（/api/orders/**）
 *
 * 权限说明：
 *  - O-02 / O-03 全部登录用户可访问，具体能看到哪些数据由服务端按角色自动限定；
 *  - O-01 / O-04 / O-06 仅居民；O-05 居民本人与管理员。
 *    服务端由 @RequireRole + JwtInterceptor + Service 层数据归属校验强制保证。
 *
 * 统一响应体与分页结构直接复用 utils/request.js 已封装的逻辑，无需重复处理。
 */

/** O-01 提交报修工单（data 中不传 residentId / status，均由服务端决定） */
export function createOrder (data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

/**
 * O-02 工单分页列表
 * @param {Object} params pageNum / pageSize / status(支持 "0,8" 多值) / categoryId /
 *                        urgency / keyword / residentId / workerId / startTime / endTime /
 *                        orderBy / orderDir
 */
export function pageOrders (params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

/** O-03 工单详情 */
export function getOrderDetail (orderId) {
  return request({
    url: `/orders/${orderId}`,
    method: 'get'
  })
}

/** O-04 修改工单（仅状态 8 待审核 / 0 待派单 可改，修改不改变状态） */
export function updateOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}`,
    method: 'put',
    data
  })
}

/** O-05 撤销工单（仅状态 8 / 0 / 9 可撤销，成功置为 7 已撤销） */
export function cancelOrder (orderId, cancelReason) {
  return request({
    url: `/orders/${orderId}/cancel`,
    method: 'put',
    data: { cancelReason }
  })
}

/** O-06 驳回工单修改后重新提交（仅状态 9 已驳回可用，成功回到 8 待审核） */
export function resubmitOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/resubmit`,
    method: 'put',
    data
  })
}
