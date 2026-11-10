import request from '@/utils/request'

/**
 * 服务评价管理模块接口（E-01 ~ E-04）
 * 对应后端 RepairEvaluationController（/api/evaluations/**）
 *
 * 权限说明（服务端由 @RequireRole + JwtInterceptor + Service 层数据范围校验强制保证）：
 *  - E-01 提交服务评价：仅居民，且必须为该工单的报修人本人（BR-6-06）；
 *  - E-02 评价分页列表：全部登录用户（居民=本人提交；维修人员=本人所接工单收到；管理员=全部）；
 *  - E-03 评价详情：全部登录用户（同上数据范围约束）；
 *  - E-04 回复评价：仅系统管理员，且一条评价只能回复一次（BR-6-07）。
 *
 * 说明：本模块**不重复封装**以下已属其他模块职责的能力（接口文档 4.7 节已明确归属）：
 *  - 「待评价工单列表」（FR-6-01）复用模块二 O-02：GET /api/orders?status=5
 *    （见 @/api/order 的 pageOrders）；
 *  - 「评价数据汇总」（FR-6-08）由模块七 S-07 统一聚合，本模块不提供统计接口；
 *  - 评价图片上传复用模块零 U-15：POST /api/files/upload（见 @/api/file 的 uploadImage）。
 */

// ==================================================================================
// 一、E-01 提交服务评价
// ==================================================================================

/**
 * E-01 提交服务评价（仅工单报修人本人可调用）
 *
 * 提交成功后工单状态由 5（已验收）置为 6（已完成），业务链路彻底闭环（BR-6-04）。
 * 综合评分 totalScore 由服务端按三项均值四舍五入计算，**不需要前端上传**。
 *
 * @param {Object} data {
 *   orderId: Number,          被评价工单 ID（必填，当前用户须为该工单报修人）,
 *   scoreService: Number,     服务态度评分 1~5（必填）,
 *   scoreSpeed: Number,       响应速度评分 1~5（必填）,
 *   scoreQuality: Number,     维修质量评分 1~5（必填）,
 *   content: String,          评价内容，0~200 字（选填）,
 *   images: String,           评价图片路径，逗号分隔，最多 3 张（选填）,
 *   isAnonymous: Number       是否匿名 0 实名 / 1 匿名，默认 0
 * }
 */
export function createEvaluation (data) {
  return request({
    url: '/evaluations',
    method: 'post',
    data
  })
}

// ==================================================================================
// 二、E-02 评价分页列表
// ==================================================================================

/**
 * E-02 评价分页列表（服务端按角色自动限定数据范围）
 *
 * @param {Object} params {
 *   pageNum, pageSize,
 *   totalScore: Number,       综合评分精确筛选 1~5,
 *   maxScore: Number,         评分上限筛选（如 maxScore=2 查低分工单）,
 *   workerId: Number,         维修人员 ID（**仅管理员可用**，越权返回 403）,
 *   categoryId: Number,       类目 ID（**仅管理员可用**，越权返回 403）,
 *   hasReply: Boolean,        是否已回复筛选,
 *   startTime, endTime        评价时间区间
 * }
 */
export function pageEvaluations (params) {
  return request({
    url: '/evaluations',
    method: 'get',
    params
  })
}

// ==================================================================================
// 三、E-03 评价详情
// ==================================================================================

/**
 * E-03 评价详情（含对应工单摘要）
 * @param {Number} evaluationId 评价 ID
 */
export function getEvaluationDetail (evaluationId) {
  return request({
    url: `/evaluations/${evaluationId}`,
    method: 'get'
  })
}

// ==================================================================================
// 四、E-04 回复评价
// ==================================================================================

/**
 * E-04 回复评价（仅管理员；一条评价只能回复一次，回复后不可修改）
 *
 * @param {Number} evaluationId 评价 ID
 * @param {Object} data { replyContent } 回复内容，必填 5~200 字
 */
export function replyEvaluation (evaluationId, data) {
  return request({
    url: `/evaluations/${evaluationId}/reply`,
    method: 'post',
    data
  })
}
