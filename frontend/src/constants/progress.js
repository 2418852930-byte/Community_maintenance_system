/**
 * 维修进度字典与常量（模块四：维修进度管理模块）
 *
 * 与后端 RecordTypeEnum / ProgressConstants 保持一一对应，
 * 是本模块前端唯一的字典来源，多个页面（我的工单列表 / 维修记录填写页）统一 import 本文件，
 * 避免各写一份导致口径不一致。
 *
 * 工单状态（10 态）字典仍复用 constants/order.js —— 状态是全局字典，
 * 本文件不重复定义，避免出现两份状态表。
 *
 * 注意：前端字典只用于「展示与界面引导」，真正的状态校验由后端 Service 层强制执行。
 */

/** 维修记录类型字典（对应 repair_record.record_type 与 RecordTypeEnum，共 6 类） */
export const RECORD_TYPE = {
  1: '接单',
  2: '到达现场',
  3: '维修进度',
  4: '更换配件',
  5: '完工',
  6: '转派退回'
}

/** 记录类型对应的 el-tag 颜色类型 */
export const RECORD_TYPE_TAG = {
  1: 'primary',
  2: 'warning',
  3: 'info',
  4: 'warning',
  5: 'success',
  6: 'danger'
}

/**
 * 记录类型的时间线节点颜色（el-timeline-item 的 type）：
 * 推进类节点为绿色，信息类为蓝色，退回类为红色。
 */
export const RECORD_TYPE_TIMELINE = {
  1: 'success',
  2: 'primary',
  3: 'primary',
  4: 'warning',
  5: 'success',
  6: 'danger'
}

/**
 * R-02「追加维修进度」允许选择的记录类型（对应后端 RecordTypeEnum.isAppendable）。
 * 1 接单、5 完工、6 转派退回分别由 D-08 / R-04 / D-09 专有接口写入，不允许在本页伪造。
 */
export const APPENDABLE_RECORD_TYPES = [
  { value: 3, label: '维修进度', hint: '常规进展说明，如「已联系住户」「等待配件到货（暂停）」' },
  { value: 2, label: '到达现场', hint: '已到达报修现场（若未走「开始维修」，也可在此补录到场节点）' },
  { value: 4, label: '更换配件', hint: '更换配件时建议填写配件名称与本次费用' }
]

/** 常用的进度说明快捷短语，降低维修人员手机端输入成本 */
export const PROGRESS_QUICK_PHRASES = [
  '已联系住户，约定上门时间',
  '已到达现场，开始排查故障',
  '等待配件到货，暂时暂停维修',
  '故障已定位，配件已更换',
  '维修已完成，等待住户验收'
]

/** 进度说明长度区间（FR-4-04：R-02） */
export const PROGRESS_CONTENT_MIN = 2
export const PROGRESS_CONTENT_MAX = 200

/** 维修结果说明长度区间（BR-4-04：R-04，必填） */
export const FINISH_CONTENT_MIN = 10
export const FINISH_CONTENT_MAX = 300

/** 到场说明长度上限（R-03） */
export const ARRIVE_REMARK_MAX = 200

/** 配件说明长度上限（对应 repair_record.parts_used VARCHAR(255)） */
export const PARTS_USED_MAX = 255

/** 维修过程 / 维修后照片张数上限（FR-4-05：最多 3 张） */
export const RECORD_IMAGES_MAX = 3

/**
 * R-02 允许追加进度的工单状态（对应后端 ProgressConstants.APPENDABLE_STATUSES）：
 * 2 已接单 / 3 维修中。状态 4 待验收及其后一律不允许追加。
 */
export const APPENDABLE_STATUSES = [2, 3]

/** R-03 允许开始维修的工单状态（BR-4-02：严格按顺序，禁止跳级） */
export const STARTABLE_STATUSES = [2]

/** R-04 允许提交维修完成的状态（BR-4-02） */
export const FINISHABLE_STATUSES = [3]

/** 记录类型中文名（容错：未知类型返回「未知」而不是 undefined） */
export function recordTypeName (recordType) {
  return RECORD_TYPE[recordType] || '未知'
}

/** 记录类型标签颜色（容错：未知类型返回 info） */
export function recordTypeTag (recordType) {
  return RECORD_TYPE_TAG[recordType] || 'info'
}

/** 记录类型时间线节点颜色 */
export function recordTypeTimeline (recordType) {
  return RECORD_TYPE_TIMELINE[recordType] || 'info'
}

/**
 * 进度时间轴的中文描述模板，用于 R-01 时间线的每一行文案。
 * 用函数接收记录对象，便于对「更换配件」这类带配件与费用的类型做特殊表达。
 */
export function recordText (record) {
  const type = record.recordType
  if (type === 1) {
    return '维修人员确认接单，进入维修链路'
  }
  if (type === 2) {
    return '到达报修现场，开始维修'
  }
  if (type === 4) {
    const parts = record.partsUsed ? `，更换：${record.partsUsed}` : ''
    return `更换配件${parts}`
  }
  if (type === 5) {
    return '维修完成，提交验收'
  }
  if (type === 6) {
    return '拒单退回，工单返回待派单池'
  }
  return '更新维修进度'
}

/** 判断某状态是否允许追加进度（前端仅用于按钮显隐，服务端才是权威校验） */
export function canAppend (status) {
  return APPENDABLE_STATUSES.includes(status)
}

/** 判断某状态是否允许开始维修 */
export function canStart (status) {
  return STARTABLE_STATUSES.includes(status)
}

/** 判断某状态是否允许提交维修完成 */
export function canFinish (status) {
  return FINISHABLE_STATUSES.includes(status)
}

/**
 * 耗时展示：分钟 -> 「x 小时 y 分钟」可读文案（R-05）。
 * null / undefined 表示该节点尚未流转，统一显示为「—」。
 */
export function durationText (minutes) {
  if (minutes === null || minutes === undefined) return '—'
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分钟`
}
