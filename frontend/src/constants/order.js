/**
 * 工单字典与常量（模块二：报修工单提交模块）
 *
 * 与后端 OrderStatusEnum / UrgencyEnum 保持一一对应，是前端唯一的状态字典来源，
 * 三个页面（提交页 / 列表页 / 详情页）统一 import 本文件，避免各页面各写一份导致口径不一致。
 *
 * 注意：前端字典只用于「展示与界面引导」，真正的状态校验由后端 Service 层强制执行。
 */

/** 工单状态字典（10 态，与接口文档 2.6 节、后端 OrderStatusEnum 完全一致） */
export const ORDER_STATUS = {
  0: '待派单',
  1: '已派单',
  2: '已接单',
  3: '维修中',
  4: '待验收',
  5: '已验收',
  6: '已完成',
  7: '已撤销',
  8: '待审核',
  9: '已驳回'
}

/** 状态对应的 el-tag 颜色类型 */
export const ORDER_STATUS_TAG = {
  0: 'warning',
  1: 'primary',
  2: 'primary',
  3: 'warning',
  4: 'warning',
  5: 'success',
  6: 'success',
  7: 'info',
  8: 'danger',
  9: 'danger'
}

/**
 * 状态筛选下拉项：按「业务时序」而非状态码大小排列。
 * 业务时序为 待审核(8) → 待派单(0) → 已派单(1) → … → 已完成(6)，
 * 旁支为 已驳回(9)、已撤销(7)。状态码大小不代表业务先后，详见接口文档 2.6 节说明。
 */
export const STATUS_OPTIONS = [
  { value: 8, label: '待审核' },
  { value: 9, label: '已驳回' },
  { value: 0, label: '待派单' },
  { value: 1, label: '已派单' },
  { value: 2, label: '已接单' },
  { value: 3, label: '维修中' },
  { value: 4, label: '待验收' },
  { value: 5, label: '已验收' },
  { value: 6, label: '已完成' },
  { value: 7, label: '已撤销' }
]

/**
 * 正常主链（用于详情页状态时间轴）：
 * 待审核(8) → 待派单(0) → 已派单(1) → 已接单(2) → 维修中(3) → 待验收(4) → 已验收(5) → 已完成(6)
 */
export const STATUS_FLOW = [8, 0, 1, 2, 3, 4, 5, 6]

/** 终态：已完成(6) 与 已撤销(7)，不可再流转 */
export const TERMINAL_STATUS = [6, 7]

/** 紧急程度字典 */
export const URGENCY = {
  0: '普通',
  1: '紧急'
}

/** 每种状态给用户的下一步说明，用于列表页与详情页的引导文案 */
export const STATUS_HINT = {
  8: '已提交，等待管理员审核；审核通过后进入待派单池',
  9: '管理员审核未通过，请查看驳回原因后修改并重新提交',
  0: '审核已通过，等待管理员派单',
  1: '已指派维修人员，等待接单',
  2: '维修人员已接单，即将上门',
  3: '维修作业进行中',
  4: '维修已完成，请在 48 小时内验收',
  5: '验收通过，请对本次服务进行评价',
  6: '工单已完成，业务闭环结束',
  7: '工单已撤销，不可再流转'
}

/** 状态中文名（容错：未知状态返回「未知」而不是 undefined） */
export function statusName (status) {
  return ORDER_STATUS[status] || '未知'
}

/** 状态标签颜色（容错：未知状态返回 info） */
export function statusTag (status) {
  return ORDER_STATUS_TAG[status] || 'info'
}

/** 紧急程度中文名 */
export function urgencyName (urgency) {
  return URGENCY[urgency] || '普通'
}
