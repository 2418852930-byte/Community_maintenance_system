import request from '@/utils/request'

/**
 * 工单验收管理模块接口（V-01 ~ V-04）
 * 对应后端 RepairVerifyController（/api/orders/**）
 *
 * 权限说明（服务端由 @RequireRole + JwtInterceptor + Service 层数据归属校验强制保证）：
 *  - V-01 待验收工单快捷查询：居民、系统管理员（居民仅返回本人提交的工单；维修人员调用返回 403）；
 *  - V-02 验收通过/驳回：仅居民，且必须为该工单的提交者本人（BR-5-06）；
 *  - V-03 管理员代验收：仅系统管理员，且工单必须已超过 48 小时待验收超时阈值（BR-5-08）；
 *  - V-04 验收记录查询：全部登录用户（居民仅本人、维修人员仅本人受派、管理员全部）。
 *
 * 说明：本模块**不重复封装**以下两个接口（接口文档 4.6 节已明确归属）：
 *  - 「查看维修结果」（FR-5-02）复用模块二 O-03（见 @/api/order 的 getOrderDetail）
 *    与模块四 R-01 维修进度时间线（见 @/api/progress 的 listOrderRecords）；
 *  - 「待验收列表」虽也可由 O-02 + status=4 实现，但接口文档 4.6.1 已提供
 *    语义化快捷接口 V-01 承载「仅看超时」与「按完工时间筛选」两项专属能力，故以 V-01 为准。
 */

// ==================================================================================
// 一、V-01 待验收工单快捷查询
// ==================================================================================

/**
 * V-01 待验收工单快捷查询（等价于 status=4，额外支持仅看超时与按完工时间筛选）
 * @param {Object} params {
 *   pageNum, pageSize,
 *   categoryId,                       按类目筛选,
 *   onlyOverdue: Boolean,             为 true 时仅返回超时未验收的工单（FR-5-06）,
 *   startTime, endTime                完工时间区间（注意：是完工时间，不是提交时间）
 * }
 */
export function pagePendingVerify (params) {
  return request({
    url: '/orders/pending-verify',
    method: 'get',
    params
  })
}

// ==================================================================================
// 二、V-02 验收通过 / 驳回
// ==================================================================================

/**
 * V-02 验收通过 / 驳回（仅工单提交者本人可调用）
 *
 *  verifyResult = 1：状态 4 → 5（已验收），解锁服务评价入口；
 *  verifyResult = 0：状态 4 → 3（维修中），原受派维修人员不变，工单回退返修。
 * 无论通过或驳回，服务端都会向 repair_verify_log 写入一行独立记录（BR-5-05）。
 *
 * @param {Number} orderId 工单 ID
 * @param {Object} data {
 *   verifyResult: 0 驳回 / 1 通过（必填）,
 *   verifyRemark: 验收意见；驳回时必填 5~200 字，通过时可选（填写则同为 5~200 字）
 * }
 */
export function verifyOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/verify`,
    method: 'put',
    data
  })
}

// ==================================================================================
// 三、V-03 管理员代验收
// ==================================================================================

/**
 * V-03 管理员代验收（仅管理员；仅支持验收通过，不支持代驳回）
 *
 * 调用前提：工单状态为 4 且已超过 48 小时待验收超时阈值（BR-5-08），
 * 未达阈值时服务端返回 409。落库时 verify_type = 2 以标识「代验收」（BR-5-07）。
 *
 * @param {Number} orderId 工单 ID
 * @param {Object} data { verifyRemark } 代验收说明，必填 5~200 字
 */
export function agentVerifyOrder (orderId, data) {
  return request({
    url: `/orders/${orderId}/verify-agent`,
    method: 'put',
    data
  })
}

// ==================================================================================
// 四、V-04 验收记录查询
// ==================================================================================

/**
 * V-04 验收记录查询（直查 repair_verify_log，按 verifyRound 升序返回全部轮次）
 * @param {Number} orderId 工单 ID
 */
export function listVerifyRecords (orderId) {
  return request({
    url: `/orders/${orderId}/verify-records`,
    method: 'get'
  })
}
