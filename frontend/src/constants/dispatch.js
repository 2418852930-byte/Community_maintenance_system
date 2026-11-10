/**
 * 派单调度字典与常量（模块三：工单派单调度模块）
 *
 * 与后端 DispatchTypeEnum / DispatchOpResultEnum / AuditResultEnum 保持一一对应，
 * 是本模块前端唯一的字典来源，多个页面统一 import 本文件，避免各写一份导致口径不一致。
 *
 * 工单状态（10 态）字典仍复用 constants/order.js —— 状态是全局字典，
 * 本文件不重复定义，避免出现两份状态表。
 *
 * 注意：前端字典只用于「展示与界面引导」，真正的状态校验由后端 Service 层强制执行。
 */

/** 派单操作类型字典（对应 repair_dispatch_log.dispatch_type 与 DispatchTypeEnum） */
export const DISPATCH_TYPE = {
  1: '派单',
  2: '改派',
  3: '发布抢单池',
  4: '抢单',
  5: '接单',
  6: '拒单'
}

/** 派单操作类型对应的 el-tag 颜色类型 */
export const DISPATCH_TYPE_TAG = {
  1: 'primary',
  2: 'warning',
  3: 'info',
  4: 'success',
  5: 'success',
  6: 'danger'
}

/** 操作结果字典（对应 repair_dispatch_log.op_result） */
export const OP_RESULT = {
  0: '失败',
  1: '成功'
}

/** 审核结果字典（对应 D-12 的 auditResult） */
export const AUDIT_RESULT = {
  0: '审核驳回',
  1: '审核通过'
}

/** 维修人员工作状态字典（对应 sys_user.work_status，仅 D-01 使用） */
export const WORK_STATUS = {
  0: '休息',
  1: '空闲',
  2: '忙碌'
}

/** 工作状态对应的 el-tag 颜色类型 */
export const WORK_STATUS_TAG = {
  0: 'info',
  1: 'success',
  2: 'warning'
}

/** 账号状态字典（对应 sys_user.status） */
export const ACCOUNT_STATUS = {
  0: '禁用',
  1: '启用'
}

/**
 * 派单轨迹的中文描述模板，用于 D-10 时间线的每一行文案。
 * 用函数接收记录对象，便于按「改派」这类涉及两人的类型做特殊表达。
 */
export function dispatchRecordText (record) {
  const type = record.dispatchType
  if (type === 1) {
    return `派单给 ${record.toWorkerName || '-'}`
  }
  if (type === 2) {
    return `改派：${record.fromWorkerName || '-'} → ${record.toWorkerName || '-'}`
  }
  if (type === 3) {
    return '发布至抢单池'
  }
  if (type === 4) {
    return `${record.toWorkerName || '-'} 抢单`
  }
  if (type === 5) {
    return `${record.toWorkerName || '-'} 接单`
  }
  if (type === 6) {
    return `${record.toWorkerName || '-'} 拒单`
  }
  return '未知操作'
}

/** 派单操作类型中文名（容错：未知类型返回「未知」而不是 undefined） */
export function dispatchTypeName (type) {
  return DISPATCH_TYPE[type] || '未知'
}

/** 派单操作类型标签颜色（容错：未知类型返回 info） */
export function dispatchTypeTag (type) {
  return DISPATCH_TYPE_TAG[type] || 'info'
}

/** 操作结果中文名 */
export function opResultName (opResult) {
  return OP_RESULT[opResult] || '未知'
}

/** 工作状态中文名 */
export function workStatusName (workStatus) {
  return WORK_STATUS[workStatus] || '未知'
}

/** 工作状态标签颜色 */
export function workStatusTag (workStatus) {
  return WORK_STATUS_TAG[workStatus] || 'info'
}

/** 审核结果中文名 */
export function auditResultName (auditResult) {
  return AUDIT_RESULT[auditResult] || '未知'
}

/**
 * 派单备注 / 改派说明 / 发布说明长度上限（对应 dispatch_remark VARCHAR(255)）
 */
export const DISPATCH_REMARK_MAX = 255

/** 审核驳回原因长度区间（BR-3-09） */
export const REJECT_REASON_MIN = 5
export const REJECT_REASON_MAX = 200

/** 拒单原因长度区间（BR-3-05） */
export const REJECT_ORDER_REASON_MIN = 5
export const REJECT_ORDER_REASON_MAX = 200

/** 接单备注长度上限 */
export const ACCEPT_REMARK_MAX = 500

/** 批量派单单次最多工单数（接口文档 4.4.3） */
export const BATCH_MAX_COUNT = 50
