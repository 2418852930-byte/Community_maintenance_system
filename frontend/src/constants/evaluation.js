/**
 * 服务评价字典与常量（模块六：服务评价管理模块）
 *
 * 与后端 EvaluationConstants 保持一一对应，是本模块前端唯一的字典来源，
 * 三个页面与两个弹窗组件统一 import 本文件，避免各写一份导致口径不一致。
 *
 * 工单状态（10 态）字典仍复用 constants/order.js —— 状态是全局字典，
 * 本文件不重复定义，避免出现两份状态表。
 *
 * 注意：前端字典只用于「展示与界面引导」，真正的状态校验与数据范围控制由后端 Service 层强制执行。
 */

/** 评分下限（1 星） */
export const SCORE_MIN = 1

/** 评分上限（5 星） */
export const SCORE_MAX = 5

/** 评分维度的固定顺序与标签（与后端 scoreService / scoreSpeed / scoreQuality 一一对应） */
export const SCORE_DIMENSIONS = [
  { key: 'scoreService', label: '服务态度', desc: '上门沟通是否礼貌、耐心' },
  { key: 'scoreSpeed', label: '响应速度', desc: '接单与上门是否及时' },
  { key: 'scoreQuality', label: '维修质量', desc: '故障是否彻底解决' }
]

/** 综合评分对应的 el-tag 颜色：4~5 星好评绿色、3 星中性、1~2 星差评红色 */
export const SCORE_TAG = {
  1: 'danger',
  2: 'danger',
  3: 'warning',
  4: 'success',
  5: 'success'
}

/** 评价内容长度上限（接口文档 4.7.1：0~200 字） */
export const CONTENT_MAX = 200

/** 评价图片张数上限（接口文档 4.7.1：最多 3 张） */
export const IMAGES_MAX = 3

/** 管理员回复长度区间（接口文档 4.7.4：5~200 字，BR-6-07） */
export const REPLY_MIN = 5
export const REPLY_MAX = 200

/**
 * 允许提交评价的工单状态（对应后端 EvaluationConstants.EVALUATABLE_STATUSES）：
 * 仅 5 已验收。工单状态 6（已完成）表示「已评价」，是评价后的终态，不可再评价。
 */
export const EVALUATABLE_STATUSES = [5]

/** 评价提交后的目标状态：6 已完成（业务闭环，BR-6-04） */
export const FINISHED_STATUS = 6

/** 是否匿名选项（对应 repair_evaluation.is_anonymous） */
export const ANONYMOUS_OPTIONS = [
  { value: 0, label: '实名评价' },
  { value: 1, label: '匿名评价' }
]

/** 匿名评价对「非本人」展示的评价人名称，与后端 EvaluationConstants.ANONYMOUS_DISPLAY_NAME 一致 */
export const ANONYMOUS_DISPLAY_NAME = '匿名用户'

/** 评分筛选下拉项（管理员评价管理页使用） */
export const SCORE_OPTIONS = [
  { value: 5, label: '5 星（非常满意）' },
  { value: 4, label: '4 星（满意）' },
  { value: 3, label: '3 星（一般）' },
  { value: 2, label: '2 星（不满意）' },
  { value: 1, label: '1 星（非常不满意）' }
]

/** 评价内容快捷短语，降低手机端输入成本（居民提交评价时使用） */
export const CONTENT_QUICK_PHRASES = [
  '上门及时，维修专业，故障已彻底解决，很满意。',
  '师傅态度好，讲解清楚，价格合理。',
  '处理过程中有耐心，问题已解决。',
  '整体还可以，但上门时间比预约晚了一些。'
]

/** 管理员回复快捷短语 */
export const REPLY_QUICK_PHRASES = [
  '感谢您的认可，我们会继续努力提供更好的服务。',
  '非常抱歉给您带来不便，我们已对该维修人员进行提醒与整改。',
  '感谢反馈，若问题仍未解决请随时再次报修，我们会优先处理。'
]

/** 评分中文说明（容错：非法值返回「未评分」） */
export function scoreText (score) {
  const map = { 1: '非常不满意', 2: '不满意', 3: '一般', 4: '满意', 5: '非常满意' }
  return map[score] || '未评分'
}

/** 综合评分标签颜色（容错：非法值返回 info） */
export function scoreTag (score) {
  return SCORE_TAG[score] || 'info'
}

/** 是否匿名中文名 */
export function anonymousText (isAnonymous) {
  return Number(isAnonymous) === 1 ? '匿名' : '实名'
}

/** 判断某工单状态是否允许评价（前端仅用于按钮显隐，服务端才是权威校验） */
export function canEvaluate (status) {
  return EVALUATABLE_STATUSES.includes(status)
}

/** 评价图片路径字符串 -> 数组（空值返回空数组） */
export function parseImages (images) {
  if (!images) return []
  return String(images)
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}
