<template>
  <div class="stat-board">
    <!-- ==========================================================================
         一、筛选栏
         统计口径：一律以工单「提交时间」为过滤依据（BR-7-01）；
         时间跨度上限 730 天，前端先行校验、后端再强制校验（BR-7-05）。
         ========================================================================== -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-item">
          <span class="filter-label">统计时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="pickerShortcuts"
            :clearable="false"
            style="width: 260px"
            @change="handleRangeChange"
          />
        </div>

        <div class="filter-item">
          <span class="filter-label">趋势粒度</span>
          <el-select v-model="query.granularity" style="width: 116px" @change="loadTrend">
            <el-option
              v-for="item in GRANULARITY_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">类目层级</span>
          <el-select v-model="query.level" style="width: 124px" @change="loadCategory">
            <el-option
              v-for="item in LEVEL_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">类目条数</span>
          <el-select v-model="query.topN" style="width: 116px" @change="loadCategory">
            <el-option
              v-for="item in TOP_N_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :loading="loading" @click="loadAll">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <el-alert
        class="filter-alert"
        type="info"
        :closable="false"
        show-icon
        :title="`统计口径：${CALIBER_NOTES.timeFilter}；${CALIBER_NOTES.maxRange}`"
      />
    </el-card>

    <!-- ==========================================================================
         二、核心指标卡
         ========================================================================== -->
    <el-row :gutter="16" class="metric-row">
      <el-col v-for="card in coreCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-card__label">
            <span>{{ card.label }}</span>
            <el-tooltip v-if="card.tip" :content="card.tip" placement="top">
              <el-icon class="metric-card__tip"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="metric-card__value" :style="{ color: card.color }">
            <span>{{ card.value }}</span>
            <span v-if="card.unit" class="metric-card__unit">{{ card.unit }}</span>
          </div>
          <div class="metric-card__extra">{{ card.extra }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==========================================================================
         三、状态计数卡（来自 S-01 的 9 项状态计数）
         ========================================================================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="block-card__header">
          <span class="block-card__title">工单状态计数</span>
          <span class="block-card__desc">
            待审核(8) 计入有效报修量；已撤销(7) 与已驳回(9) 单独成列，不计入
          </span>
        </div>
      </template>
      <div class="status-counts">
        <div v-for="item in statusCountCards" :key="item.key" class="status-count">
          <div class="status-count__dot" :style="{ backgroundColor: item.color }"></div>
          <div class="status-count__name">{{ item.label }}</div>
          <div class="status-count__num" :style="{ color: item.color }">{{ item.value }}</div>
        </div>
      </div>
    </el-card>

    <!-- ==========================================================================
         四、图表区：折线图（趋势） + 柱状图（人员工作量）
         ========================================================================== -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="block-card">
          <template #header>
            <div class="block-card__header">
              <span class="block-card__title">报修趋势（{{ granularityName(query.granularity) }}）</span>
              <span class="block-card__desc">
                区间累计 {{ trendTotal }} 件 · 按工单提交时间统计
              </span>
            </div>
          </template>
          <BaseChart
            :option="trendOption"
            :empty="trendEmpty"
            :loading="loadingTrend"
            height="340px"
            empty-text="所选区间内暂无报修工单"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="block-card">
          <template #header>
            <div class="block-card__header">
              <span class="block-card__title">报修类目占比</span>
              <span class="block-card__desc">
                有效报修量 {{ categoryTotal }} 件
              </span>
            </div>
          </template>
          <BaseChart
            :option="categoryOption"
            :empty="categoryEmpty"
            :loading="loadingCategory"
            height="340px"
            empty-text="所选区间内暂无类目数据"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- ==========================================================================
         五、柱状图：维修人员工作量排名
         ========================================================================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="block-card__header">
          <span class="block-card__title">维修人员工作量排名</span>
          <span class="block-card__desc">
            按完成量降序；{{ CALIBER_NOTES.workerScope }} · 参与统计 {{ workerList.length }} 人
          </span>
        </div>
      </template>
      <BaseChart
        :option="workerOption"
        :empty="workerEmpty"
        :loading="loadingWorker"
        height="360px"
        empty-text="所选区间内暂无维修人员工作量数据"
      />

      <el-table
        v-if="!workerEmpty"
        :data="workerList"
        size="small"
        class="worker-table"
        :header-cell-style="{ backgroundColor: '#fafafa' }"
      >
        <el-table-column label="排名" type="index" width="64" align="center" />
        <el-table-column prop="workerName" label="维修人员" min-width="100" />
        <el-table-column prop="workerType" label="工种" min-width="90" />
        <el-table-column prop="acceptCount" label="接单量" width="80" align="center" />
        <el-table-column prop="finishCount" label="完成量" width="80" align="center" />
        <el-table-column prop="processingCount" label="在途量" width="80" align="center" />
        <el-table-column prop="cancelCount" label="撤销量" width="80" align="center" />
        <el-table-column label="平均维修时长" width="120" align="center">
          <template #default="{ row }">{{ formatMinutes(row.avgRepairMinutes) }}</template>
        </el-table-column>
        <el-table-column label="平均评分" width="90" align="center">
          <template #default="{ row }">{{ formatScore(row.avgScore) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ==========================================================================
         六、状态分布表（S-04，恒为 0~9 共 10 项）
         ========================================================================== -->
    <el-card shadow="never" class="block-card">
      <template #header>
        <div class="block-card__header">
          <span class="block-card__title">工单状态分布</span>
          <span class="block-card__desc">
            {{ CALIBER_NOTES.totalOrderCount }}
          </span>
        </div>
      </template>
      <el-table
        :data="statusList"
        size="small"
        v-loading="loadingStatus"
        :header-cell-style="{ backgroundColor: '#fafafa' }"
      >
        <el-table-column prop="status" label="状态码" width="80" align="center" />
        <el-table-column prop="statusName" label="状态名称" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.statusName || statusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="工单数量" width="110" align="center" />
        <el-table-column label="占有效报修量" min-width="220">
          <template #default="{ row }">
            <div class="ratio-cell">
              <el-progress
                :percentage="Number(row.ratio) || 0"
                :stroke-width="10"
                :show-text="false"
                :color="STATUS_CHART_COLORS[row.status] || '#409EFF'"
                class="ratio-cell__bar"
              />
              <span class="ratio-cell__text">{{ formatRatio(row.ratio) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="180">
          <template #default="{ row }">{{ statusHint(row.status) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 报修统计分析看板（模块七主页面，S-01 ~ S-05）。
 *
 * <p>对应需求 FR-7-01 ~ FR-7-04、FR-7-08、FR-7-10；接口文档 4.8.1 ~ 4.8.5。
 * 仅系统管理员可访问（路由 meta.roles = ['admin']，服务端另有强制校验）。</p>
 *
 * <h3>页面结构（自上而下）</h3>
 * <ol>
 *   <li><b>筛选栏</b>：统计时间范围（核心筛选，BR-7-01）+ 趋势粒度 + 类目层级 / 条数；</li>
 *   <li><b>核心指标卡</b>：有效报修量、维修完成率、今日新增、平均综合评分（S-01）；</li>
 *   <li><b>状态计数卡</b>：S-01 的 9 项状态计数；</li>
 *   <li><b>折线图</b>：报修趋势（S-02，默认按月）；</li>
 *   <li><b>饼图</b>：报修类目占比（S-03，环形图）；</li>
 *   <li><b>柱状图 + 排名表</b>：维修人员工作量（S-05）；</li>
 *   <li><b>状态分布表</b>：S-04 的 10 态完整分布。</li>
 * </ol>
 * <p>图表类型恰为「折线图 + 饼图 + 柱状图」三种，与交付要求一一对应，未额外新增。</p>
 *
 * <h3>数据加载策略</h3>
 * <p>五个接口相互独立，用 <b>{@code Promise.allSettled}</b> 并发请求：
 * 某个接口失败（如 403 / 网络抖动）只影响它自己对应的模块，
 * 其余四个照常渲染，而不是整页空白——这对「大屏看板」这类页面尤其重要。
 * 请求失败时的错误提示由 {@code utils/request.js} 的响应拦截器统一弹出，
 * 本页不重复弹窗。</p>
 *
 * <h3>时间范围校验</h3>
 * <p>后端对超过 {@link MAX_RANGE_DAYS} 天的区间返回 400（BR-7-05）。
 * 本页在发请求前先做一次同样的前端校验，避免「无意义的网络往返 + 400 弹窗」，
 * 但<b>不替代</b>后端校验（前端校验只是体验优化，NFR-S-03）。</p>
 */

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/BaseChart.vue'
import {
  getCategoryDistribution,
  getOverview,
  getStatusDistribution,
  getTrend,
  getWorkerWorkload
} from '@/api/statistics'
import {
  CALIBER_NOTES,
  CHART_COLORS,
  DEFAULT_GRANULARITY,
  DEFAULT_TOP_N,
  GRANULARITY_OPTIONS,
  LEVEL_OPTIONS,
  LEVEL_PRIMARY,
  MAX_RANGE_DAYS,
  RANGE_PRESETS,
  STATUS_CHART_COLORS,
  TOP_N_OPTIONS,
  buildYearRange,
  granularityName,
  statusName
} from '@/constants/statistics'

// ==================================================================================
// 一、筛选条件
// ==================================================================================

/**
 * 默认时间范围：本年度 [当年 1 月 1 日, 今天]。
 *
 * <p>与后端 S-01 / S-03 ~ S-05 的默认起始时间口径一致；
 * 跨度最长 366 天，天然满足 730 天上限，首次进入页面不会触发 400。</p>
 */
const dateRange = ref(buildYearRange())

/** 查询条件（与 StatisticsQueryDTO 字段同名，便于直接透传给接口） */
const query = reactive({
  granularity: DEFAULT_GRANULARITY,
  level: LEVEL_PRIMARY,
  topN: DEFAULT_TOP_N
})

/** el-date-picker 快捷选项：预设天数换算成 [Date, Date]（组件要求返回 Date 对象） */
const pickerShortcuts = RANGE_PRESETS.map((preset) => ({
  text: preset.label,
  value: () => {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - preset.days)
    return [start, end]
  }
}))

/** 由 dateRange 组装出接口所需的 startTime / endTime */
const timeParams = computed(() => {
  if (!Array.isArray(dateRange.value) || dateRange.value.length !== 2) {
    return {}
  }
  return {
    startTime: dateRange.value[0],
    endTime: dateRange.value[1]
  }
})

// ==================================================================================
// 二、响应式数据
// ==================================================================================

const overview = ref({})
const trend = ref({ granularity: DEFAULT_GRANULARITY, total: 0, list: [] })
const category = ref({ total: 0, list: [] })
const status = ref({ total: 0, list: [] })
const workerList = ref([])

const loading = ref(false)
const loadingTrend = ref(false)
const loadingCategory = ref(false)
const loadingStatus = ref(false)
const loadingWorker = ref(false)

// ==================================================================================
// 三、格式化工具
// ==================================================================================

/** 千分位整数（无值显示 0） */
function formatInt (value) {
  if (value === null || value === undefined) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN')
}

/** 百分比（保留 2 位小数 + %），无值显示 0.00% */
function formatPercent (value) {
  if (value === null || value === undefined) {
    return '0.00%'
  }
  return `${Number(value).toFixed(2)}%`
}

/** 比例（非百分数，保留 2 位小数；S-04 的 ratio 实际是百分数，此处按接口原值展示） */
function formatRatio (value) {
  if (value === null || value === undefined) {
    return '0.00%'
  }
  return `${Number(value).toFixed(2)}%`
}

/** 分钟（四舍五入到整数 + 单位），无样本显示「—」而不是 0，避免与「真的耗时 0 分钟」混淆 */
function formatMinutes (value) {
  if (value === null || value === undefined) {
    return '—'
  }
  return `${Number(value)} 分钟`
}

/** 评分（1 位小数），无评价显示「—」（对齐接口文档 4.8.1「无评价时为 null」） */
function formatScore (value) {
  if (value === null || value === undefined) {
    return '—'
  }
  return Number(value).toFixed(1)
}

/** el-tag 颜色类型：沿用模块二 ORDER_STATUS_TAG 的语义色 */
function statusTagType (statusCode) {
  switch (statusCode) {
    case 5:
    case 6:
      return 'success'
    case 7:
      return 'info'
    case 8:
    case 9:
      return 'danger'
    default:
      return 'warning'
  }
}

/** 状态说明（与模块二 STATUS_HINT 同语义，此处只保留一句短提示） */
function statusHint (statusCode) {
  const hints = {
    0: '审核通过，等待派单',
    1: '已指派维修人员',
    2: '维修人员已接单',
    3: '维修作业进行中',
    4: '维修完成，等待验收',
    5: '验收通过，等待评价',
    6: '已评价，业务闭环结束',
    7: '居民撤销，不计入有效报修量',
    8: '已提交，等待管理员审核',
    9: '审核驳回，不计入有效报修量'
  }
  return hints[statusCode] || '—'
}

// ==================================================================================
// 四、指标卡
// ==================================================================================

/** 核心指标卡（有效报修量 / 维修完成率 / 今日新增 / 平均评分） */
const coreCards = computed(() => {
  const data = overview.value || {}
  return [
    {
      key: 'total',
      label: '有效报修量',
      value: formatInt(data.totalOrderCount),
      unit: '件',
      color: CHART_COLORS[0],
      tip: CALIBER_NOTES.totalOrderCount,
      extra: `区间内提交，已排除无效工单`
    },
    {
      key: 'rate',
      label: '维修完成率',
      value: data.completedRate === null || data.completedRate === undefined
        ? '0.00%'
        : formatPercent(data.completedRate),
      unit: '',
      color: CHART_COLORS[1],
      tip: CALIBER_NOTES.completedRate,
      extra: `完成量 ${formatInt((data.verifiedCount || 0) + (data.completedCount || 0))} 件`
    },
    {
      key: 'today',
      label: '今日新增工单',
      value: formatInt(data.todayOrderCount),
      unit: '件',
      color: CHART_COLORS[2],
      tip: '口径：今日 00:00:00 之后提交的全部未删除工单，与所选时间范围无关',
      extra: '含待审核，独立于所选区间'
    },
    {
      key: 'score',
      label: '平均综合评分',
      value: formatScore(data.avgScore),
      unit: data.avgScore === null || data.avgScore === undefined ? '' : '分',
      color: CHART_COLORS[3],
      tip: CALIBER_NOTES.avgScore,
      extra: `评价率 ${data.evaluatedRate === null || data.evaluatedRate === undefined
        ? '0.00'
        : Number(data.evaluatedRate).toFixed(2)}`
    }
  ]
})

/** 状态计数卡：待审核 / 待派单 / 处理中 / 待验收 / 已验收 / 已完成 / 已撤销 / 已驳回 */
const statusCountCards = computed(() => {
  const data = overview.value || {}
  const items = [
    { key: 'pendingAudit', label: '待审核 (8)', value: data.pendingAuditCount, color: STATUS_CHART_COLORS[8] },
    { key: 'pendingDispatch', label: '待派单 (0)', value: data.pendingDispatchCount, color: STATUS_CHART_COLORS[0] },
    { key: 'processing', label: '处理中 (1~4)', value: data.processingCount, color: CHART_COLORS[0] },
    { key: 'pendingVerify', label: '待验收 (4)', value: data.pendingVerifyCount, color: STATUS_CHART_COLORS[4] },
    { key: 'verified', label: '已验收 (5)', value: data.verifiedCount, color: STATUS_CHART_COLORS[5] },
    { key: 'completed', label: '已完成 (6)', value: data.completedCount, color: STATUS_CHART_COLORS[6] },
    { key: 'cancel', label: '已撤销 (7)', value: data.cancelCount, color: STATUS_CHART_COLORS[7] },
    { key: 'rejected', label: '已驳回 (9)', value: data.rejectedCount, color: STATUS_CHART_COLORS[9] }
  ]
  return items.map((item) => ({ ...item, value: formatInt(item.value) }))
})

// ==================================================================================
// 五、图表 option
// ==================================================================================

const trendEmpty = computed(() => !trend.value.list || trend.value.list.length === 0)
const trendTotal = computed(() => formatInt(trend.value.total))

/**
 * 折线图 option：报修总量 / 完成量 / 已撤销 / 已驳回四条曲线。
 * 后端已把区间内的**每一天/周/月**补齐（无数据补 0），因此折线不会在中间断开。
 */
const trendOption = computed(() => {
  const list = trend.value.list || []
  const labels = list.map((item) => item.timeLabel)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: ['报修总量', '完成量', '已撤销', '已驳回'] },
    grid: { left: 56, right: 24, top: 46, bottom: 46, containLabel: false },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLabel: { rotate: labels.length > 12 ? 35 : 0, fontSize: 11 }
    },
    yAxis: { type: 'value', minInterval: 1, name: '工单数' },
    series: [
      {
        name: '报修总量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: list.map((item) => item.orderCount),
        itemStyle: { color: CHART_COLORS[0] },
        areaStyle: { opacity: 0.08 }
      },
      {
        name: '完成量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: list.map((item) => item.completedCount),
        itemStyle: { color: CHART_COLORS[1] }
      },
      {
        name: '已撤销',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: list.map((item) => item.cancelCount),
        itemStyle: { color: CHART_COLORS[4] }
      },
      {
        name: '已驳回',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: list.map((item) => item.rejectedCount),
        itemStyle: { color: CHART_COLORS[3] }
      }
    ]
  }
})

const categoryEmpty = computed(() => !category.value.list || category.value.list.length === 0)
const categoryTotal = computed(() => formatInt(category.value.total))

/** 饼图（环形）option：报修类目占比 */
const categoryOption = computed(() => {
  const list = category.value.list || []
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.name}<br/>工单量：${params.value} 件（${params.percent}%）`
    },
    legend: { type: 'scroll', orient: 'vertical', right: 8, top: 'middle', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['38%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#ffffff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
        labelLine: { length: 8, length2: 8 },
        data: list.map((item, index) => ({
          name: item.categoryName || `类目 ${item.categoryId}`,
          value: Number(item.orderCount) || 0,
          itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] }
        }))
      }
    ]
  }
})

const workerEmpty = computed(() => !workerList.value || workerList.value.length === 0)

/** 柱状图 option：维修人员工作量排名（接单量 / 完成量 / 在途量三组柱） */
const workerOption = computed(() => {
  const list = workerList.value || []
  const names = list.map((item) => item.workerName || `#${item.workerId}`)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, data: ['接单量', '完成量', '在途量'] },
    grid: { left: 56, right: 24, top: 46, bottom: 56 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { rotate: names.length > 6 ? 30 : 0, fontSize: 11 }
    },
    yAxis: { type: 'value', minInterval: 1, name: '工单数' },
    series: [
      {
        name: '接单量',
        type: 'bar',
        barMaxWidth: 26,
        data: list.map((item) => item.acceptCount),
        itemStyle: { color: '#A0CFFF' }
      },
      {
        name: '完成量',
        type: 'bar',
        barMaxWidth: 26,
        data: list.map((item) => item.finishCount),
        itemStyle: { color: CHART_COLORS[1] }
      },
      {
        name: '在途量',
        type: 'bar',
        barMaxWidth: 26,
        data: list.map((item) => item.processingCount),
        itemStyle: { color: CHART_COLORS[2] }
      }
    ]
  }
})

/** 状态分布表数据（后端已保证 0~9 共 10 项、按状态码升序） */
const statusList = computed(() => status.value.list || [])

// ==================================================================================
// 六、数据加载
// ==================================================================================

/** 请求公共参数：时间范围 */
function baseParams () {
  return { ...timeParams.value }
}

/**
 * 前端时间跨度校验。
 * @returns {Boolean} true 表示通过
 */
function checkRange () {
  const params = timeParams.value
  if (!params.startTime || !params.endTime) {
    ElMessage.warning('请选择统计时间范围')
    return false
  }
  const start = new Date(`${params.startTime}T00:00:00`)
  const end = new Date(`${params.endTime}T23:59:59`)
  const days = Math.floor((end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000))
  if (days > MAX_RANGE_DAYS) {
    ElMessage.warning(`查询时间跨度不得超过 ${MAX_RANGE_DAYS} 天，当前为 ${days} 天`)
    return false
  }
  return true
}

/** S-01 统计概览 */
async function loadOverview () {
  const res = await getOverview(baseParams())
  overview.value = res.data || {}
}

/** S-02 报修趋势 */
async function loadTrend () {
  loadingTrend.value = true
  try {
    const res = await getTrend({ ...baseParams(), granularity: query.granularity })
    trend.value = res.data || { granularity: query.granularity, total: 0, list: [] }
  } finally {
    loadingTrend.value = false
  }
}

/** S-03 类目分布 */
async function loadCategory () {
  loadingCategory.value = true
  try {
    const res = await getCategoryDistribution({
      ...baseParams(),
      level: query.level,
      topN: query.topN
    })
    category.value = res.data || { total: 0, list: [] }
  } finally {
    loadingCategory.value = false
  }
}

/** S-04 状态分布 */
async function loadStatus () {
  loadingStatus.value = true
  try {
    const res = await getStatusDistribution(baseParams())
    status.value = res.data || { total: 0, list: [] }
  } finally {
    loadingStatus.value = false
  }
}

/** S-05 维修人员工作量 */
async function loadWorker () {
  loadingWorker.value = true
  try {
    const res = await getWorkerWorkload(baseParams())
    workerList.value = res.data || []
  } finally {
    loadingWorker.value = false
  }
}

/**
 * 加载全部五个接口（并发 + 互不影响）。
 * 用 allSettled 而非 all：单个接口失败不应该让整页数据消失。
 */
async function loadAll () {
  if (!checkRange()) {
    return
  }
  loading.value = true
  await Promise.allSettled([
    loadOverview(),
    loadTrend(),
    loadCategory(),
    loadStatus(),
    loadWorker()
  ])
  loading.value = false
}

/** 时间范围变更：五个接口都受时间范围影响，因此整体重载 */
function handleRangeChange () {
  loadAll()
}

/** 重置筛选条件为默认值（本年度 + 按月 + 一级类目 + topN 10）并重新加载 */
function handleReset () {
  dateRange.value = buildYearRange()
  query.granularity = DEFAULT_GRANULARITY
  query.level = LEVEL_PRIMARY
  query.topN = DEFAULT_TOP_N
  loadAll()
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.stat-board {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 筛选栏 ---------- */
.filter-card :deep(.el-card__body) {
  padding-bottom: 12px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.filter-alert {
  margin-top: 12px;
}

/* ---------- 指标卡 ---------- */
.metric-row {
  margin-bottom: 0;
}

.metric-card__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.metric-card__tip {
  cursor: help;
  color: #c0c4cc;
}

.metric-card__value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}

.metric-card__unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
}

.metric-card__extra {
  margin-top: 6px;
  font-size: 12px;
  color: #a8abb2;
}

/* ---------- 通用区块卡片 ---------- */
.block-card {
  width: 100%;
}

.block-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.block-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.block-card__desc {
  font-size: 12px;
  color: #909399;
}

/* ---------- 状态计数卡 ---------- */
.status-counts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.status-count {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background-color: #fafafa;
  border-radius: 6px;
}

.status-count__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-count__name {
  flex: 1;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.status-count__num {
  font-size: 18px;
  font-weight: 600;
}

/* ---------- 图表行 ---------- */
.chart-row {
  margin-bottom: 0;
}

/* ---------- 表格 ---------- */
.worker-table {
  margin-top: 12px;
}

.ratio-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ratio-cell__bar {
  flex: 1;
}

.ratio-cell__text {
  width: 56px;
  text-align: right;
  font-size: 12px;
  color: #606266;
}

/* Element Plus 默认栅格在换行后需要下间距 */
:deep(.el-col) {
  margin-bottom: 0;
}

@media (max-width: 992px) {
  .el-col {
    margin-bottom: 16px;
  }
}
</style>
