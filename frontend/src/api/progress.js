import request from '@/utils/request'

/**
 * 维修进度管理模块接口（R-01 ~ R-05）
 * 对应后端 RepairProgressController（/api/orders/**）
 *
 * 权限说明（服务端由 @RequireRole + JwtInterceptor + Service 层数据归属校验强制保证）：
 *  - R-01 进度时间线查询：全部登录用户（居民仅本人、维修人员仅本人受派、管理员全部）；
 *  - R-02 追加维修进度 / R-03 开始维修 / R-04 提交维修完成：仅维修人员，且须为该工单受派人；
 *  - R-05 维修耗时统计：管理员与受派维修人员。
 *
 * 说明：本模块**不重复封装**以下两个接口（接口文档 4.5 节已明确归属）：
 *  - 「我的工单列表」（FR-4-01）复用模块二 O-02，见 @/api/order 的 pageOrders；
 *  - 「接单」（FR-4-02）复用模块三 D-08，见 @/api/dispatch 的 acceptOrder。
 */

// ==================================================================================
// 一、R-01 进度时间线查询
// ==================================================================================

/**
 * R-01 进度时间线查询（按 recordTime 升序返回工单全部进度节点）
 * @param {Number} orderId 工单 ID
 * @param {Number} [recordType] 记录类型筛选 1~6，不传表示不筛选
 */
export function listOrderRecords (orderId, recordType) {
  return request({
    url: `/orders/${orderId}/records`,
    method: 'get',
    params: { recordType: recordType === null || recordType === '' ? undefined : recordType }
  })
}

// ==================================================================================
// 二、R-02 追加维修进度
// ==================================================================================

/**
 * R-02 追加维修进度（不改变工单状态，本次费用自动累加至工单 total_cost）
 * @param {Number} orderId 工单 ID
 * @param {Object} data {
 *   recordType: 2 到达现场 / 3 维修进度 / 4 更换配件,
 *   progressContent: 进度说明（2~200 字，必填）,
 *   images: 维修过程照片路径，逗号分隔，最多 3 张,
 *   partsUsed: 使用/更换的配件（≤255 字）,
 *   cost: 本次费用（元，默认 0，不可为负）,
 *   recordTime: 记录发生时间（可选，默认服务端当前时间）
 * }
 */
export function appendOrderRecord (orderId, data) {
  return request({
    url: `/orders/${orderId}/records`,
    method: 'post',
    data
  })
}

// ==================================================================================
// 三、R-03 开始维修
// ==================================================================================

/**
 * R-03 开始维修（状态 2 → 3，自动生成「到达现场」记录；禁止跳级）
 * @param {Number} orderId 工单 ID
 * @param {Object} [data] { remark } 到场说明，可选，≤ 200 字
 */
export function startRepair (orderId, data) {
  return request({
    url: `/orders/${orderId}/start`,
    method: 'put',
    data: data || {}
  })
}

// ==================================================================================
// 四、R-04 提交维修完成
// ==================================================================================

/**
 * R-04 提交维修完成（状态 3 → 4 待验收，生成「完工」记录；本接口是模块五的输入契约）
 * @param {Number} orderId 工单 ID
 * @param {Object} data {
 *   progressContent: 维修结果说明（10~300 字，必填）,
 *   images: 维修后照片路径，逗号分隔，最多 3 张,
 *   partsUsed: 配件汇总（≤255 字）,
 *   cost: 本次完工结算费用（元）
 * }
 */
export function finishRepair (orderId, data) {
  return request({
    url: `/orders/${orderId}/finish`,
    method: 'put',
    data
  })
}

// ==================================================================================
// 五、R-05 维修耗时统计
// ==================================================================================

/** R-05 维修耗时统计（BR-7-02 口径：派单/接单/维修/验收/全流程五项耗时） */
export function getOrderDuration (orderId) {
  return request({
    url: `/orders/${orderId}/duration`,
    method: 'get'
  })
}
