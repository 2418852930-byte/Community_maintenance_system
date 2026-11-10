import request from '@/utils/request'

/**
 * 报修统计分析模块接口（S-01 ~ S-05）
 * 对应后端 StatisticsController（/api/statistics/**）
 *
 * 权限说明：本模块五个接口**全部仅系统管理员可调用**（BR-7-07），
 * 服务端由 `JwtInterceptor` + `StatisticsController` 类级 `@RequireRole(RoleEnum.ADMIN)`
 * 强制校验，Service 层另有角色兜底断言。非管理员调用一律返回 403，
 * 前端隐藏菜单只是体验优化，不能作为权限依据（NFR-S-03）。
 *
 * 数据口径（与后端 StatisticsConstants / 数据库附录 C 完全一致，前端不重复定义口径）：
 *  - 所有统计一律按工单**提交时间**（repair_order.create_time）过滤（BR-7-01）；
 *  - 有效报修量 = 全部 − 已撤销(7) − 已驳回(9)，**含待审核(8)**（BR-7-04）；
 *  - 完成量 = 已验收(5) + 已完成(6)；在途量 = 已派单(1) + 已接单(2) + 维修中(3) + 待验收(4)；
 *  - 单次查询时间跨度上限 730 天，超限返回 400（BR-7-05）。
 *
 * 时间入参：同时兼容 `yyyy-MM-dd HH:mm:ss` 与 `yyyy-MM-dd`，
 * 只传日期时「起」自动补 00:00:00、「止」自动补 23:59:59（与模块六 E-02 一致）。
 */

// ==================================================================================
// 一、S-01 统计概览看板
// ==================================================================================

/**
 * S-01 统计概览看板
 *
 * @param {Object} params {
 *   startTime: String,  统计时间起（选填，缺省「当年 1 月 1 日」），
 *   endTime: String     统计时间止（选填，缺省「当前时间」）
 * }
 * @returns {Promise<Object>} data 结构：
 *   {
 *     totalOrderCount, todayOrderCount, pendingAuditCount, pendingDispatchCount,
 *     processingCount, pendingVerifyCount, verifiedCount, completedCount,
 *     cancelCount, rejectedCount,
 *     completedRate,      维修完成率（%，2 位小数）= 完成量(5+6) ÷ 有效报修量
 *     avgScore,           平均综合评分（1 位小数，无评价为 null）
 *     avgRepairMinutes,   平均维修时长（分钟）
 *     avgAcceptMinutes,   平均接单时长（分钟）
 *     evaluatedRate       评价率（比例，非百分数，2 位小数）
 *   }
 */
export function getOverview (params) {
  return request({
    url: '/statistics/overview',
    method: 'get',
    params
  })
}

// ==================================================================================
// 二、S-02 报修量趋势统计
// ==================================================================================

/**
 * S-02 报修量趋势统计（供折线图使用）
 *
 * @param {Object} params {
 *   granularity: String, 统计粒度 day / week / month，默认 day,
 *   startTime: String,   缺省「近 30 天」,
 *   endTime: String,     缺省「当前时间」,
 *   categoryId: Number,  类目筛选（选填，选中父类目时连同子类目一并纳入）,
 *   workerId: Number,    维修人员筛选（选填）,
 *   urgency: Number      紧急程度 0 普通 / 1 紧急（选填）
 * }
 * @returns {Promise<Object>} data 结构：
 *   {
 *     granularity: String,    回显的实际粒度,
 *     total: Number,          区间内提交的工单总数,
 *     list: [{ timeLabel, orderCount, completedCount, cancelCount, rejectedCount }]
 *   }
 *   区间内无任何工单时 list 为空数组、total 为 0。
 */
export function getTrend (params) {
  return request({
    url: '/statistics/trend',
    method: 'get',
    params
  })
}

// ==================================================================================
// 三、S-03 类目分布统计
// ==================================================================================

/**
 * S-03 类目分布统计（供饼图 / 环形图使用）
 *
 * @param {Object} params {
 *   level: Number,       类目聚合层级 1 一级类目（默认）/ 2 二级类目,
 *   topN: Number,        返回前 N 项，默认 10、上限 50,
 *   startTime: String,   缺省「当年 1 月 1 日」,
 *   endTime: String,     缺省「当前时间」,
 *   categoryId: Number,  类目筛选（选填）,
 *   workerId: Number     维修人员筛选（选填）
 * }
 * @returns {Promise<Object>} data 结构：
 *   {
 *     total: Number,  有效报修量（各项 ratio 的统一分母，独立于 topN 截断）,
 *     list: [{ categoryId, categoryName, orderCount, completedCount, ratio }]
 *   }
 *   说明：topN 截断长尾类目时，各项 ratio 之和会小于 100%，属预期行为。
 */
export function getCategoryDistribution (params) {
  return request({
    url: '/statistics/category',
    method: 'get',
    params
  })
}

// ==================================================================================
// 四、S-04 工单状态分布统计
// ==================================================================================

/**
 * S-04 工单状态分布统计
 *
 * @param {Object} params {
 *   startTime: String,   缺省「当年 1 月 1 日」,
 *   endTime: String,     缺省「当前时间」,
 *   categoryId: Number,  类目筛选（选填）,
 *   workerId: Number     维修人员筛选（选填）
 * }
 * @returns {Promise<Object>} data 结构：
 *   {
 *     total: Number,  有效报修量,
 *     list: [{ status, statusName, orderCount, ratio }]  // 恒为 0 ~ 9 共 10 项，按状态码升序
 *   }
 */
export function getStatusDistribution (params) {
  return request({
    url: '/statistics/status',
    method: 'get',
    params
  })
}

// ==================================================================================
// 五、S-05 维修人员工作量统计
// ==================================================================================

/**
 * S-05 维修人员工作量统计（供柱状图与排名表使用）
 *
 * @param {Object} params {
 *   startTime: String,   缺省「当年 1 月 1 日」,
 *   endTime: String,     缺省「当前时间」,
 *   workerId: Number,    只统计指定维修人员（选填，不传返回全部排名）,
 *   categoryId: Number   类目筛选（选填）
 * }
 * @returns {Promise<Array>} data 为人员数组（服务端已按完成量降序、接单量降序排好序）：
 *   [{
 *     workerId, workerName, workerType,
 *     acceptCount, finishCount, processingCount, cancelCount,
 *     avgRepairMinutes,   // 无样本时为 null
 *     avgScore            // 无评价时为 null
 *   }]
 *   无数据时为空数组（不是 404）。
 */
export function getWorkerWorkload (params) {
  return request({
    url: '/statistics/worker',
    method: 'get',
    params
  })
}
