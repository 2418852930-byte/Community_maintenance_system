import { ORDER_STATUS } from '@/constants/order'

/**
 * 报修统计分析模块常量（模块七）。
 *
 * <p>本文件是模块七**唯一的前端字典来源**，与后端 `StatisticsConstants` /
 * `OrderStatusEnum` 保持一一对应；「看板页」「图表组件」统一 import 本文件，
 * 避免各页面各写一份导致口径不一致。</p>
 *
 * <p><b>状态字典刻意不在此重复定义</b>：{@link ORDER_STATUS} 直接复用模块二
 * `@/constants/order` 的 10 态字典，保证「统计分析」页与「我的报修」「工单审核」
 * 等页面显示的同一工单状态名**逐字相同**（与后端「状态字典只有 `OrderConstants`
 * 一处定义」的做法同构）。</p>
 *
 * <p>注意：前端常量只用于「展示与界面引导」，真正的取值域校验与统计口径
 * 由后端 Service 层强制执行。</p>
 */

// ==================================================================================
// 一、统计粒度（对应接口文档 4.8.2 granularity）
// ==================================================================================

/** 统计粒度字典 */
export const GRANULARITY = {
  day: '按日',
  week: '按周',
  month: '按月'
}

/**
 * 粒度下拉项。
 * 顺序按「由细到粗」排列，默认选中「按月」，与交付要求中「月度报修趋势折线图」一致。
 */
export const GRANULARITY_OPTIONS = [
  { value: 'day', label: '按日' },
  { value: 'week', label: '按周' },
  { value: 'month', label: '按月' }
]

/** 折线图默认粒度：按月（交付要求「月度报修趋势」） */
export const DEFAULT_GRANULARITY = 'month'

/** 粒度中文名（容错：未知粒度返回原值） */
export function granularityName (granularity) {
  return GRANULARITY[granularity] || granularity
}

// ==================================================================================
// 二、类目聚合层级（对应接口文档 4.8.3 level）
// ==================================================================================

/** 一级类目 */
export const LEVEL_PRIMARY = 1

/** 二级类目 */
export const LEVEL_SECONDARY = 2

/** 类目层级下拉项 */
export const LEVEL_OPTIONS = [
  { value: LEVEL_PRIMARY, label: '一级类目' },
  { value: LEVEL_SECONDARY, label: '二级类目' }
]

/** 类目分布默认返回条数（接口文档 4.8.3：默认 10） */
export const DEFAULT_TOP_N = 10

/** 类目分布可选的 topN 取值（上限 50，与后端 MAX_TOP_N 一致） */
export const TOP_N_OPTIONS = [
  { value: 5, label: '前 5 项' },
  { value: 10, label: '前 10 项' },
  { value: 20, label: '前 20 项' }
]

// ==================================================================================
// 三、时间范围（BR-7-01 一律按工单提交时间过滤；BR-7-05 跨度上限 730 天）
// ==================================================================================

/** 单次查询允许的最大时间跨度（天），与后端 MAX_RANGE_DAYS 一致 */
export const MAX_RANGE_DAYS = 730

/**
 * 时间范围快捷预设（el-date-picker 的 `:shortcuts` 数据源）。
 *
 * <p>每个预设只声明「距今天数」，由页面把它换算成 `[Date, Date]` 交给组件
 * （el-date-picker 的 shortcuts.value 需要返回 Date 对象，组件再按
 * `value-format="YYYY-MM-DD"` 转成字符串回填到 daterange）。</p>
 *
 * <p>只传日期时后端会自动把「起」补 00:00:00、「止」补 23:59:59，
 * 因此前端无需拼时分秒（与模块六 E-02 的处理方式一致）。</p>
 *
 * <p>四个预设的跨度分别为 7 / 30 / 90 / 365 天，全部在 730 天上限内，
 * 不会触发后端 400。</p>
 */
export const RANGE_PRESETS = [
  { label: '近 7 天', days: 6 },
  { label: '近 30 天', days: 29 },
  { label: '近 90 天', days: 89 },
  { label: '近一年', days: 364 }
]

/**
 * 把 Date 格式化为 `yyyy-MM-dd`（本地时区，不走 toISOString，避免时区偏移串日）。
 * @param {Date} date
 * @returns {String} 如 '2026-09-21'
 */
export function formatDate (date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 本年度起始日 [当年 1 月 1 日, 今天] —— 与后端 S-01 / S-03 ~ S-05 的默认起始时间口径一致 */
export function buildYearRange () {
  const now = new Date()
  return [`${now.getFullYear()}-01-01`, formatDate(now)]
}

// ==================================================================================
// 四、统计口径说明（用于页面上的「口径提示」气泡）
//     逐条对齐需求 BR-7-01 ~ BR-7-05 与数据库附录 C，前端只做「转述展示」，不自定义口径
// ==================================================================================

/** 口径说明条目：key 为指标名，value 为口径描述 */
export const CALIBER_NOTES = {
  totalOrderCount: '有效报修量 = 全部工单 − 已撤销(7) − 已驳回(9)，其中待审核(8) 计入（BR-7-04）',
  completedRate: '维修完成率 = 完成量(已验收5 + 已完成6) ÷ 有效报修量 × 100%',
  timeFilter: '所有统计一律以工单的提交时间（create_time）为过滤依据（BR-7-01）',
  maxRange: `单次查询时间跨度不得超过 ${MAX_RANGE_DAYS} 天（BR-7-05）`,
  avgScore: '平均综合评分仅统计已产生评价的工单，未评价工单不计入分母（BR-7-03）',
  workerScope: '待审核(8) / 已驳回(9) / 已撤销(7) 未进入派单链路，不计入任何维修人员指标',
  topN: '类目占比的分母为独立查询得到的有效报修量；topN 截断长尾类目时各项占比之和小于 100%'
}

// ==================================================================================
// 五、图表配置（ECharts）
// ==================================================================================

/**
 * 看板统一配色。
 *
 * <p>柱状图 / 饼图 / 折线图共用同一套色板，保证三个图在视觉上属于同一套设计语言。</p>
 */
export const CHART_COLORS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#53A8FF', '#85CE61', '#F3D19E', '#F89898', '#A6A9AD'
]

/**
 * 状态分布的柱条配色（10 态）。
 *
 * <p>沿用模块二 `ORDER_STATUS_TAG` 的语义色：进行中=黄、成功=绿、无效=灰/红，
 * 使「统计分析」页的状态色与「我的报修」页的状态标签色保持一致。</p>
 */
export const STATUS_CHART_COLORS = {
  0: '#E6A23C',
  1: '#409EFF',
  2: '#409EFF',
  3: '#E6A23C',
  4: '#E6A23C',
  5: '#67C23A',
  6: '#67C23A',
  7: '#909399',
  8: '#F56C6C',
  9: '#F56C6C'
}

/** 状态中文名：优先使用后端返回的 statusName，缺失时回退到本地字典（容错返回「未知」） */
export function statusName (status) {
  return ORDER_STATUS[status] || '未知'
}
