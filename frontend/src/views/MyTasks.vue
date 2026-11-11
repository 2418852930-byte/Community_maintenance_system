<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明 ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页为维修人员<b>我的工单</b>：只展示<b>指派给本人的</b>工单（服务端按当前登录身份自动限定，
        无法查看他人工单）。请按 <b>已派单(1) → 已接单(2) → 维修中(3) → 待验收(4)</b> 的顺序推进，
        系统禁止跳级；维修中的工单可多次追加进度，填写完成后提交验收。
      </el-alert>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待接单" name="dispatched" />
        <el-tab-pane label="维修中" name="repairing" />
        <el-tab-pane label="待验收" name="verify" />
        <el-tab-pane label="已完结" name="finished" />
      </el-tabs>

      <el-form :model="query" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="工单编号 / 标题 / 地址 / 联系人"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="query.urgency" placeholder="全部" clearable style="width: 120px">
            <el-option label="普通" :value="0" />
            <el-option label="紧急" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-tag :type="tabTagType" effect="dark" size="large">{{ tabLabel }} {{ total }} 条</el-tag>
        <span class="toolbar-tip">{{ tabTip }}</span>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
        <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="报修类目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="address" label="维修地址" min-width="170" show-overflow-tooltip />
        <el-table-column label="联系人 / 电话" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.contactName || '-' }}<br />
            <span class="sub-text">{{ row.contactPhone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="紧急" width="76" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
            <span v-else class="sub-text">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="期望上门" width="150" align="center">
          <template #default="{ row }">{{ row.expectTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="94" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ row.statusName || statusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="费用合计" width="100" align="right">
          <template #default="{ row }">
            <span class="cost">¥ {{ Number(row.totalCost || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" class-name="op-cell">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>

            <!-- 状态 1 已派单：仅「接单」（复用模块三 D-08），接单后进入维修链路 -->
            <el-button
              v-if="isDispatched(row.status)"
              link
              type="success"
              size="small"
              @click="openAccept(row)"
            >接单</el-button>

            <!-- 状态 2 已接单：开始维修（R-03），禁止跳级 -->
            <el-button
              v-if="canStart(row.status)"
              link
              type="warning"
              size="small"
              @click="handleStart(row)"
            >开始维修</el-button>

            <!-- 状态 2 / 3：追加维修进度（R-02） -->
            <el-button
              v-if="canAppend(row.status)"
              link
              type="primary"
              size="small"
              @click="openProgress(row)"
            >更新进度</el-button>

            <!-- 状态 3 维修中：填写维修记录并提交完成（R-04） -->
            <el-button
              v-if="canFinish(row.status)"
              link
              type="success"
              size="small"
              @click="goFinish(row)"
            >提交完工</el-button>

            <el-button link type="info" size="small" @click="openRecords(row)">进度时间轴</el-button>
            <el-button
              v-if="row.status >= 1"
              link
              type="info"
              size="small"
              @click="openDuration(row)"
            >耗时</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="emptyText" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>

    <!-- ==================== 接单弹窗（复用模块三 D-08，备注可选） ==================== -->
    <el-dialog v-model="acceptVisible" title="接单确认" width="520px">
      <el-alert type="success" :closable="false" show-icon style="margin-bottom: 12px">
        接单后工单状态变为「已接单(2)」，并自动生成维修进度时间线的首个节点。
        请按居民期望时间上门，随后点击「开始维修」进入维修中。
      </el-alert>

      <el-form label-width="90px">
        <el-form-item label="工单编号"><span>{{ acceptForm.orderNo }}</span></el-form-item>
        <el-form-item label="报修标题"><span>{{ acceptForm.title }}</span></el-form-item>
        <el-form-item label="上门地址"><span>{{ acceptForm.address }}</span></el-form-item>
        <el-form-item label="接单备注">
          <el-input
            v-model="acceptForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可选，≤500 字。例：预计今日下午 15:00 上门，请保持电话畅通。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleAcceptSubmit">确认接单</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 开始维修弹窗（R-03，到场说明可选） ==================== -->
    <el-dialog v-model="startVisible" title="开始维修" width="520px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        确认后工单状态由「已接单(2)」变更为「<b>维修中(3)</b>」，并自动生成一条「到达现场」记录，
        该时间将作为「维修时长」的起点。请确认已到达报修现场。
      </el-alert>

      <el-form label-width="90px">
        <el-form-item label="工单编号"><span>{{ startForm.orderNo }}</span></el-form-item>
        <el-form-item label="报修标题"><span>{{ startForm.title }}</span></el-form-item>
        <el-form-item label="到场说明">
          <el-input
            v-model="startForm.remark"
            type="textarea"
            :rows="3"
            :maxlength="ARRIVE_REMARK_MAX"
            show-word-limit
            placeholder="可选，≤200 字。例：已到达现场，正在排查水槽下方渗水点。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="handleStartSubmit">确认开始维修</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 进度更新弹窗（R-02 公共组件） ==================== -->
    <ProgressUpdateDialog
      v-model="progressVisible"
      :order-id="currentOrder.orderId"
      :order-no="currentOrder.orderNo"
      @success="fetchList"
    />

    <!-- ==================== 维修耗时弹窗（R-05） ==================== -->
    <el-dialog v-model="durationVisible" title="维修耗时明细" width="620px">
      <div v-loading="durationLoading">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
          口径依据 BR-7-02：维修时长 = 完工时间 − 开始维修时间（首条「到达现场」记录时间）。
          尚未流转到的节点显示「—」。
        </el-alert>

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="工单编号">{{ duration.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ duration.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="派单时间">{{ duration.dispatchTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ duration.acceptTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="开始维修">{{ duration.startTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ duration.finishTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="验收时间">{{ duration.verifyTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="派单时长">
            {{ durationText(duration.dispatchDurationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="接单时长">
            {{ durationText(duration.acceptDurationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="维修时长">
            {{ durationText(duration.repairDurationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="验收时长">
            {{ durationText(duration.verifyDurationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="全流程时长">
            <b>{{ durationText(duration.totalDurationMinutes) }}</b>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="durationVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 进度时间轴抽屉（R-01 公共组件） ==================== -->
    <OrderRecordTimeline v-model="recordsVisible" :order-id="recordsOrderId" :order-no="recordsOrderNo" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders } from '@/api/order'
import { acceptOrder } from '@/api/dispatch'
import { getOrderDuration, startRepair } from '@/api/progress'
import { statusName, statusTag } from '@/constants/order'
import { ARRIVE_REMARK_MAX, canAppend, canFinish, canStart, durationText } from '@/constants/progress'
import ProgressUpdateDialog from '@/components/ProgressUpdateDialog.vue'
import OrderRecordTimeline from '@/components/OrderRecordTimeline.vue'

/**
 * 维修人员「我的工单」列表（模块四 · FR-4-01）
 *
 * 数据来源：<b>复用模块二 O-02</b>（GET /api/orders）。接口文档 4.5 节开头的模块边界约定明确
 * 「FR-4-01 我的工单列表由 O-02 按维修人员数据范围实现」，因此本页不新建后端接口 ——
 * 服务端在 O-02 中已强制注入 worker_id = 当前登录用户，本页无需（也无法）传任何归属参数。
 *
 * 「接单」按钮复用模块三 D-08（PUT /api/orders/{orderId}/accept），本模块不重复实现，
 * 只是把入口放到维修人员最常用的「我的工单」列表里。
 *
 * 四个标签页按状态切分：
 *  ① 待接单 = 1（已派单）        ② 维修中 = 2,3（已接单 / 维修中）
 *  ③ 待验收 = 4                  ④ 已完结 = 5,6（已验收 / 已完成）
 * 状态取值域只覆盖维修链路（1~6），待审核(8) / 已驳回(9) / 待派单(0) 不指派给维修人员，
 * 服务端虽会因 worker_id 限定而自然过滤，这里仍显式限定以保持口径清晰。
 */
const router = useRouter()

/** 标签页 -> 工单状态集合（多值以英文逗号分隔，O-02 原生支持） */
const TAB_STATUS = {
  dispatched: { status: '1', label: '待接单', tag: 'warning',
    tip: '管理员已派单，请及时点击「接单」确认；接单后工单进入维修链路' },
  repairing: { status: '2,3', label: '维修中', tag: 'primary',
    tip: '可「更新进度」（可多次，不改变状态）；维修完成后点「提交完工」流转为待验收' },
  verify: { status: '4', label: '待验收', tag: 'warning',
    tip: '维修已完成，等待居民验收；验收驳回后工单将回到「维修中」，可再次提交完工' },
  finished: { status: '5,6', label: '已完结', tag: 'success',
    tip: '验收通过或评价完成的工单，仅可查看进度时间轴与耗时明细' }
}

const activeTab = ref('repairing')
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  urgency: null
})

/** 当前标签页的展示文案 */
const tabConfig = computed(() => TAB_STATUS[activeTab.value] || TAB_STATUS.repairing)
const tabLabel = computed(() => tabConfig.value.label)
const tabTagType = computed(() => tabConfig.value.tag)
const tabTip = computed(() => tabConfig.value.tip)
const emptyText = computed(() => `「${tabLabel.value}」暂无工单`)

/** 状态 1 已派单：可接单（复用 D-08） */
function isDispatched (status) {
  return status === 1
}

async function fetchList () {
  loading.value = true
  try {
    // O-02：服务端自动限定为「本人受派工单」，此处只传状态 / 关键字 / 紧急程度
    const res = await pageOrders({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: tabConfig.value.status,
      keyword: query.keyword || undefined,
      urgency: query.urgency === null || query.urgency === '' ? undefined : query.urgency
    })
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange () {
  query.pageNum = 1
  fetchList()
}

function handleSearch () {
  query.pageNum = 1
  fetchList()
}

function handleReset () {
  query.keyword = ''
  query.urgency = null
  query.pageNum = 1
  fetchList()
}

function goDetail (row) {
  // 模块二 O-03 工单详情（维修人员仅可查看本人受派工单）
  router.push(`/orders/${row.orderId}`)
}

// ======================= 接单（复用模块三 D-08） =======================
const acceptVisible = ref(false)
const acceptForm = reactive({ orderId: null, orderNo: '', title: '', address: '', remark: '' })

function openAccept (row) {
  acceptForm.orderId = row.orderId
  acceptForm.orderNo = row.orderNo
  acceptForm.title = row.title
  acceptForm.address = row.address
  acceptForm.remark = ''
  acceptVisible.value = true
}

async function handleAcceptSubmit () {
  submitting.value = true
  try {
    const res = await acceptOrder(acceptForm.orderId, { remark: acceptForm.remark.trim() || undefined })
    ElMessage.success(res.message || '接单成功，工单已进入「已接单」')
    acceptVisible.value = false
    fetchList()
  } catch (e) {
    // 权限 / 状态冲突提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ======================= 开始维修（R-03） =======================
const startVisible = ref(false)
const startForm = reactive({ orderId: null, orderNo: '', title: '', remark: '' })

function handleStart (row) {
  startForm.orderId = row.orderId
  startForm.orderNo = row.orderNo
  startForm.title = row.title
  startForm.remark = ''
  startVisible.value = true
}

async function handleStartSubmit () {
  submitting.value = true
  try {
    const res = await startRepair(startForm.orderId, { remark: startForm.remark.trim() || undefined })
    ElMessage.success(res.message || '已开始维修，工单状态变更为「维修中」')
    startVisible.value = false
    fetchList()
  } catch (e) {
    // 409「请按顺序更新进度」等提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ======================= 更新进度（R-02 公共组件） =======================
const progressVisible = ref(false)
const currentOrder = reactive({ orderId: null, orderNo: '' })

function openProgress (row) {
  currentOrder.orderId = row.orderId
  currentOrder.orderNo = row.orderNo
  progressVisible.value = true
}

// ======================= 提交完工（跳转维修记录填写页，R-04） =======================
function goFinish (row) {
  // hidden 路由：维修记录填写页（R-02 追加进度 + R-04 提交完工）
  router.push(`/my-tasks/${row.orderId}/record`)
}

// ======================= 维修耗时（R-05） =======================
const durationVisible = ref(false)
const durationLoading = ref(false)
const duration = ref({})

async function openDuration (row) {
  durationVisible.value = true
  durationLoading.value = true
  duration.value = {}
  try {
    const res = await getOrderDuration(row.orderId)
    duration.value = res.data || {}
  } catch (e) {
    duration.value = {}
  } finally {
    durationLoading.value = false
  }
}

// ======================= 进度时间轴（R-01 公共组件） =======================
const recordsVisible = ref(false)
const recordsOrderId = ref(null)
const recordsOrderNo = ref('')

function openRecords (row) {
  recordsOrderId.value = row.orderId
  recordsOrderNo.value = row.orderNo
  recordsVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.toolbar-tip {
  font-size: 12px;
  color: #909399;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}

.cost {
  color: #e6a23c;
  font-weight: 600;
}

/* 操作列：按钮强制单行显示，避免被挤压折行 */
:deep(.op-cell .cell) {
  white-space: nowrap !important;
  word-break: keep-all !important;
}

:deep(.op-cell .el-button) {
  margin-left: 0 !important;
  margin-right: 10px;
}

:deep(.op-cell .el-button:last-child) {
  margin-right: 0;
}
</style>
