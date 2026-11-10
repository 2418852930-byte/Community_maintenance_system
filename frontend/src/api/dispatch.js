import request from '@/utils/request'

/**
 * 工单派单调度模块接口（D-01 ~ D-12）
 * 对应后端 RepairDispatchController（/api/orders/**）与 WorkerController（/api/workers/**）
 *
 * 权限说明（服务端由 @RequireRole + JwtInterceptor + Service 层数据归属校验强制保证）：
 *  - D-01 / D-02 / D-03 / D-04 / D-05 / D-11 / D-12：仅系统管理员；
 *  - D-06 / D-07 / D-08 / D-09：仅维修人员（D-08 / D-09 还须为该工单的受派人）；
 *  - D-10：管理员与受派维修人员。
 *
 * 说明：待审核工单列表（管理员审核工作台）复用模块二的 O-02 接口
 * pageOrders({ status: 8 })，接口文档 Q11 已确认「待审核池」不单独建接口，
 * 因此本文件不重复封装该查询。
 */

// ==================================================================================
// 一、工单审核（D-12）
// ==================================================================================

/**
 * D-12 工单审核（通过 / 驳回）
 * @param {Number} orderId 工单 ID
 * @param {Object} data { auditResult: 1 通过 / 0 驳回, rejectReason: 驳回原因（驳回时必填 5~200 字） }
 */
export function auditOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/audit`,
    method: 'put',
    data
  })
}

// ==================================================================================
// 二、管理员派单工作台（D-01 ~ D-05、D-11）
// ==================================================================================

/**
 * D-11 待派单池快捷查询（等价于 O-02 且 status = 0，紧急工单置顶）
 * @param {Object} params pageNum / pageSize / categoryId / urgency / startTime / endTime
 */
export function pagePendingDispatch (params) {
  return request({
    url: '/orders/pending-dispatch',
    method: 'get',
    params
  })
}

/**
 * D-02 派单给维修人员
 * @param {Number} orderId 工单 ID
 * @param {Object} data { workerId, dispatchRemark }
 */
export function dispatchOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/dispatch`,
    method: 'put',
    data
  })
}

/**
 * D-03 批量派单（1~50 条，全批共用同一维修人员）
 * @param {Object} data { orderIds: Array<Number>, workerId, dispatchRemark }
 */
export function batchDispatch (data) {
  return request({
    url: '/orders/batch-dispatch',
    method: 'put',
    data
  })
}

/**
 * D-04 改派（仅「已派单且未接单」的工单可用）
 * @param {Number} orderId 工单 ID
 * @param {Object} data { workerId, dispatchRemark }
 */
export function reassignOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/reassign`,
    method: 'put',
    data
  })
}

/**
 * D-05 发布抢单池（仅状态 0 可发布，发布后状态仍为 0）
 * @param {Number} orderId 工单 ID
 * @param {Object} data { dispatchRemark }
 */
export function publishGrabPool (orderId, data) {
  return request({
    url: `/orders/${orderId}/publish-grab`,
    method: 'put',
    data
  })
}

/**
 * D-01 维修人员负载视图（辅助均衡派单；也是派单弹窗的选人数据源）
 * @param {Object} params { workerType, onlyEnabled }
 */
export function getWorkerWorkload (params) {
  return request({
    url: '/workers/workload',
    method: 'get',
    params
  })
}

// ==================================================================================
// 三、维修人员侧（D-06 ~ D-09）
// ==================================================================================

/**
 * D-06 抢单池列表（仅已发布且未被领取的待派单工单）
 * @param {Object} params pageNum / pageSize / categoryId / urgency / addressKeyword
 */
export function pageGrabPool (params) {
  return request({
    url: '/orders/grab-pool',
    method: 'get',
    params
  })
}

/** D-07 抢单（先到先得，并发冲突返回 409） */
export function grabOrder (orderId) {
  return request({
    url: `/orders/${orderId}/grab`,
    method: 'put'
  })
}

/**
 * D-08 接单（状态 1 → 2，同时生成进度时间线首个节点）
 * @param {Object} data { remark } 接单备注可选，≤ 500 字
 */
export function acceptOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/accept`,
    method: 'put',
    data: data || {}
  })
}

/**
 * D-09 拒单（状态 1 → 0 回退，拒单原因必填 5~200 字）
 * @param {Object} data { rejectReason }
 */
export function rejectOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/reject`,
    method: 'put',
    data
  })
}

// ==================================================================================
// 四、派单记录查询（D-10）
// ==================================================================================

/** D-10 派单记录查询（按 dispatchTime 升序返回完整派单轨迹） */
export function listDispatchRecords (orderId) {
  return request({
    url: `/orders/${orderId}/dispatch-records`,
    method: 'get'
  })
}
