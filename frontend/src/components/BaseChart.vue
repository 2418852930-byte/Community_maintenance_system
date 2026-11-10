<template>
  <div class="base-chart" :style="{ height }">
    <!-- ECharts 画布容器：始终挂载，保证从「空数据」切到「有数据」时尺寸已就绪 -->
    <div ref="chartRef" class="base-chart__canvas"></div>

    <!-- 空数据占位遮罩：覆盖在画布之上，避免出现「空白图 + 缺省动画」的观感 -->
    <div v-if="empty" class="base-chart__empty">
      <el-empty :image-size="72" :description="emptyText" />
    </div>
  </div>
</template>

<script setup>
/**
 * ECharts 通用封装组件（模块七新增）。
 *
 * <p>本组件是模块七唯一的图表容器，承担四件事，让业务页只需要「给 option」：</p>
 * <ol>
 *   <li><b>初始化与销毁</b>：{@code onMounted} 里 {@code init}，{@code onBeforeUnmount} 里
 *       {@code dispose}，避免路由切换后残留实例导致内存泄漏（ECharts 实例持有 canvas，泄漏代价高）；</li>
 *   <li><b>自动自适应</b>：用 {@code ResizeObserver} 监听容器尺寸变化（侧边栏折叠、窗口缩放、
 *       内容区滚动条出现都会触发），而不是监听 {@code window.resize} ——
 *       {@code ResizeObserver} 能捕捉到「窗口没变但容器变了」的场景；</li>
 *   <li><b>空数据占位</b>：由父组件传入 {@code empty}，本组件渲染 el-empty 遮罩，
 *       业务页不必在每个图旁边各写一段空态判断；</li>
 *   <li><b>加载态</b>：透传 ECharts 内置的 {@code showLoading} / {@code hideLoading}。</li>
 * </ol>
 *
 * <p><b>为什么用按需引入而不是 {@code import * as echarts from 'echarts'}？</b>
 * 整包 echarts 会打进约 1MB 的代码，而看板实际只用到「柱状图 + 饼图 + 折线图」
 * 三种图表与少量组件。这里从 {@code echarts/core} 显式注册所需模块，
 * 构建产物显著更小，是 ECharts 5 官方推荐用法。</p>
 *
 * <p><b>为什么不在这里调用接口？</b>本组件刻意做成<b>纯展示组件</b>（受控组件），
 * 只接收 {@code option} 并渲染，不感知任何业务与请求。
 * 这样它可被任意页面复用，也便于单独调试。</p>
 */

import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册：只引入看板真正用到的图表与组件
echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer
])

const props = defineProps({
  /** ECharts 配置对象（必填）。父组件用 computed 生成，本组件只负责渲染 */
  option: {
    type: Object,
    required: true
  },
  /** 容器高度，支持任意 CSS 长度 */
  height: {
    type: String,
    default: '340px'
  },
  /** 是否处于空数据状态：为 true 时显示占位遮罩 */
  empty: {
    type: Boolean,
    default: false
  },
  /** 空数据文案 */
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  /** 是否显示 ECharts 内置加载动画 */
  loading: {
    type: Boolean,
    default: false
  }
})

const chartRef = ref(null)
/**
 * 图表实例用 shallowRef 持有。
 * ECharts 实例内部结构庞大且自带响应式代理会造成性能问题与难以排查的行为，
 * 因此不做深层响应式（官方在 Vue 集成示例中亦如此建议）。
 */
const chart = shallowRef(null)
/** ResizeObserver 实例，用于卸载时断开监听 */
let resizeObserver = null

/**
 * 渲染/更新图表。
 * 使用 notMerge = true：让新 option 完全替换旧配置，
 * 避免「上一次筛选残留的 series 与新数据合并」导致图表出现幽灵曲线。
 */
function render () {
  if (!chart.value) {
    return
  }
  chart.value.setOption(props.option, true)
  // setOption 后容器尺寸可能因父级布局变化而失效，主动校准一次
  chart.value.resize()
}

onMounted(() => {
  chart.value = echarts.init(chartRef.value)
  render()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (chart.value) {
        chart.value.resize()
      }
    })
    resizeObserver.observe(chartRef.value)
  }
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

watch(
  () => props.loading,
  (value) => {
    if (!chart.value) {
      return
    }
    if (value) {
      chart.value.showLoading({
        text: '加载中',
        color: '#409EFF',
        textColor: '#606266',
        maskColor: 'rgba(255, 255, 255, 0.8)',
        zlevel: 0
      })
    } else {
      chart.value.hideLoading()
    }
  }
)

onBeforeUnmount(() => {
  // ① 断开尺寸监听 ② 销毁实例释放 canvas，二者缺一都会造成内存泄漏
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chart.value) {
    chart.value.dispose()
    chart.value = null
  }
})
</script>

<style scoped>
.base-chart {
  position: relative;
  width: 100%;
}

.base-chart__canvas {
  width: 100%;
  height: 100%;
}

.base-chart__empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}
</style>
