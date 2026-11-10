/**
 * 工单验收字典与常量（模块五：工单验收管理模块）
 *
 * 与后端 VerifyTypeEnum / VerifyResultEnum / VerifyConstants 保持一一对应，
 * 是本模块前端唯一的字典来源，列表页与三个弹窗组件统一 import 本文件，
 * 避免各写一份导致口径不一致。
 *
 * 工单状态（10 态）字典仍复用 constants/order.js —— 状态是全局字典，
 * 本文件不重复定义，避免出现两份状态表；
 * 维修记录类型字典仍复用 constants/progress.js（验收时查看维修结果会用到）。
 *
 * 注意：前端字典只用于「展示与界面引导」，真正的状态校验由后端 Service 层强制执行。
 */

/** 验收结果字典（对应 repair_verify_log.verify_result 与 VerifyResultEnum） */
export const VERIFY_RESULT = {
  0: '驳回',
  1: '通过'
}

/** 验收结果对应的 el-tag 颜色类型：驳回用红色告警，通过用绿色 */
export const VERIFY_RESULT_TAG = {
  0: 'danger',
  1: 'success'
}

/** 验收类型字典（对应 repair_verify_log.verify_type 与 VerifyTypeEnum） */
export const VERIFY_TYPE = {
  1: '居民验收',
  2: '管理员代验收'
}

/** 验收类型对应的 el-tag 颜色类型：代验收用醒目色以便与居民验收区分（BR-5-07） */
export const VERIFY_TYPE_TAG = {
  1: 'primary',
  2: 'warning'
}

/**
 * 验收记录时间线的节点颜色（el-timeline-item 的 type）：
 * 通过为绿色（推进），驳回为红色（回退）。
 */
export const VERIFY_RESULT_TIMELINE = {
  0: 'danger',
  1: 'success'
}

/**
 * 允许验收的工单状态（对应后端 VerifyConstants.VERIFIABLE_STATUSES）：
 * 仅 4 待验收。其余状态一律 409，前端据此隐藏按钮。
 */
export const VERIFIABLE_STATUSES = [4]

/** 验收通过后的目标状态：5 已验收（解锁模块六评价入口） */
export const VERIFIED_STATUS = 5

/** 验收驳回后的目标状态：3 维修中（回退模块四返修） */
export const REJECTED_BACK_STATUS = 3

/** 验收意见（驳回原因）长度区间：驳回时必填 5~200 字（BR-5-03） */
export const VERIFY_REMARK_MIN = 5
export const VERIFY_REMARK_MAX = 200

/** 代验收说明长度区间：必填 5~200 字（BR-5-07） */
export const AGENT_REMARK_MIN = 5
export const AGENT_REMARK_MAX = 200

/** 待验收超时阈值（小时），与后端 OrderConstants.VERIFY_OVERDUE_HOURS 保持同口径（BR-5-08） */
export const OVERDUE_HOURS = 48

/** 常用的驳回原因快捷短语，降低居民手机端输入成本 */
export const REJECT_QUICK_PHRASES = [
  '故障未彻底解决，仍有异常现象，请重新检查',
  '更换的配件与报修描述不符，请核实后重新处理',
  '维修后现场未清理，请一并处理',
  '维修效果不理想，与报修前相比无明显改善'
]

/** 常用的验收意见快捷短语（通过时可选填写） */
export const PASS_QUICK_PHRASES = [
  '已恢复正常使用，服务及时',
  '维修师傅态度好，处理专业',
  '问题已解决，无异常'
]

/** 验收结果中文名（容错：未知结果返回「未知」而不是 undefined） */
export function verifyResultName (verifyResult) {
  return VERIFY_RESULT[verifyResult] || '未知'
}

/** 验收结果标签颜色（容错：未知结果返回 info） */
export function verifyResultTag (verifyResult) {
  return VERIFY_RESULT_TAG[verifyResult] || 'info'
}

/** 验收结果时间线节点颜色 */
export function verifyResultTimeline (verifyResult) {
  return VERIFY_RESULT_TIMELINE[verifyResult] || 'info'
}

/** 验收类型中文名 */
export function verifyTypeName (verifyType) {
  return VERIFY_TYPE[verifyType] || '未知'
}

/** 验收类型标签颜色 */
export function verifyTypeTag (verifyType) {
  return VERIFY_TYPE_TAG[verifyType] || 'info'
}

/** 判断某状态是否允许验收（前端仅用于按钮显隐，服务端才是权威校验） */
export function canVerify (status) {
  return VERIFIABLE_STATUSES.includes(status)
}

/**
 * 待验收剩余时间文案：根据完工时间计算距离 48 小时超时阈值还剩多久。
 *
 * - 未超时：返回「剩余 xx 小时」（向下取整，最小显示 < 1 小时）；
 * - 已超时：返回「已超时 xx 小时」，供列表高亮提示（FR-5-06）。
 *
 * @param {String} finishTime 完工时间（yyyy-MM-dd HH:mm:ss）
 * @returns {String} 文案；入参为空或格式非法时返回 '—'
 */
export function overdueText (finishTime) {
  if (!finishTime) return '—'
  // Safari 等浏览器对 'yyyy-MM-dd HH:mm:ss' 解析不稳定，统一替换为 ISO 兼容写法
  const start = new Date(String(finishTime).replace(/-/g, '/')).getTime()
  if (Number.isNaN(start)) return '—'
  const elapsedHours = (Date.now() - start) / 3600000
  if (elapsedHours >= OVERDUE_HOURS) {
    return `已超时 ${Math.floor(elapsedHours - OVERDUE_HOURS)} 小时`
  }
  const remain = Math.ceil(OVERDUE_HOURS - elapsedHours)
  return remain <= 0 ? '即将超时' : `剩余 ${remain} 小时`
}
