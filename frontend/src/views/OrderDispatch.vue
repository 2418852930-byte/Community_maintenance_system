<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明 ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页为管理员<b>派单工作台</b>：承接「审核通过后」的工单（状态 0 待派单），
        在此指派维修人员；派单成功后工单状态流转为「已派单（1）」，自动关联维修人员 ID，
        并流入下游「维修进度管理模块」的维修人员工单池。已派单且维修人员<b>尚未接单</b>的工单可在第二个标签页改派。
      </el-alert>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================== 标签一：待派单池（D-11 / D-02 / D-03 / D-05） ==================== -->
        <el-tab-pane label="待派单池" name="pending">
          <el-form :model="query" inline>
            <el-form-item label="报修类目">
              <el-select v-model="query.categoryId" placeholder="全部类目" clearable style="width: 200px">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="紧急程度">
              <el-select v-model="query.urgency" placeholder="全部" clearable style="width: 120px">
                <el-option label="普通" :value="0" />
                <el-option label="紧急" :value="1" />
              </el-select>
            </el-form-item>

            <el-form-item label="提交时间">
              <el-date-picker
                v-model="timeRange"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 360px"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
              <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 工具条：批量派单入口（D-03） -->
          <div class="toolbar">
            <el-tag type="warning" effect="dark" size="large">待派单 {{ total }} 条</el-tag>
            <el-button
              type="primary"
              :icon="Promotion"
              :disabled="selection.length === 0"
              @click="openBatchDispatch"
            >
              批量派单（已选 {{ selection.length }}）
            </el-button>
            <span class="toolbar-tip">
              紧急工单自动置顶（BR-3-07）；勾选多行可一次指派给同一维修人员，单次最多 50 条
            </span>
          </div>

          <el-table
            v-loading="loading"
            :data="tableData"
            border
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="46" align="center" />
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="报修类目" min-width="150" show-overflow-tooltip />
            <el-table-column prop="address" label="维修地址" min-width="170" show-overflow-tooltip />
            <el-table-column label="报修居民" width="120" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.residentName || '-' }}<br />
                <span class="sub-text">{{ row.contactPhone }}</span>
              </template>
            </el-table-column>
            <el-table-column label="紧急" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
                <span v-else class="sub-text">普通</span>
              </template>
            </el-table-column>
            <el-table-column label="期望上门" width="160" align="center">
              <template #default="{ row }">{{ row.expectTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small" effect="dark">
                  {{ row.statusName || statusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="270">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
                <el-button link type="success" size="small" @click="openDispatch(row)">派单</el-button>
                <el-button link type="warning" size="small" @click="openPublish(row)">发布抢单池</el-button>
                <el-button link type="info" size="small" @click="openRecords(row)">派单轨迹</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无待派单工单，请先到「工单审核」页审核居民提交的报修" />
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
        </el-tab-pane>

        <!-- ==================== 标签二：已派单待接单（D-04 改派） ==================== -->
        <el-tab-pane label="已派单待接单" name="dispatched">
          <el-form :model="query2" inline>
            <el-form-item label="关键字">
              <el-input
                v-model="query2.keyword"
                placeholder="工单编号 / 标题 / 地址 / 联系人"
                clearable
                style="width: 220px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
              <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="toolbar">
            <el-tag type="primary" effect="dark" size="large">已派单待接单 {{ total }} 条</el-tag>
            <span class="toolbar-tip">
              仅「已派单（1）且维修人员尚未接单」的工单可改派（D-04）；一旦维修人员接单（状态 2），改派入口关闭
            </span>
          </div>

          <el-table v-loading="loading" :data="tableData" style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="报修类目" min-width="150" show-overflow-tooltip />
            <el-table-column prop="address" label="维修地址" min-width="170" show-overflow-tooltip />
            <el-table-column prop="workerName" label="当前维修人员" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.workerName || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="紧急" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
                <span v-else class="sub-text">普通</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small" effect="dark">
                  {{ row.statusName || statusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
                <el-button link type="warning" size="small" @click="openReassign(row)">改派</el-button>
                <el-button link type="info" size="small" @click="openRecords(row)">派单轨迹</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无已派单待接单工单" />
            </template>
          </el-table>

          <el-pagination
            v-model:current-page="query2.pageNum"
            v-model:page-size="query2.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="fetchList"
            @current-change="fetchList"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ==================== 派单 / 批量派单 / 改派 弹窗（D-01 选人 + D-02 / D-03 / D-04） ==================== -->
    <el-dialog
      v-model="dispatchVisible"
      :title="dispatchTitle"
      width="880px"
      top="6vh"
      @open="fetchWorkerWorkload"
    >
      <el-alert :type="dialogAlertType" :closable="false" show-icon style="margin-bottom: 12px">
        {{ dialogAlertText }}
      </el-alert>

      <!-- 待处理工单摘要 -->
      <el-descriptions :column="dispatchMode === 'batch' ? 1 : 2" border size="small" style="margin-bottom: 12px">
        <template v-if="dispatchMode === 'batch'">
          <el-descriptions-item label="待派单工单">
            <el-tag
              v-for="item in batchRows"
              :key="item.orderId"
              size="small"
              style="margin: 2px 4px 2px 0"
            >
              {{ item.orderNo }}
            </el-tag>
            <div class="sub-text" style="margin-top: 6px">
              共 {{ batchRows.length }} 条，将同时指派给下方选中的维修人员
            </div>
          </el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="报修标题">{{ currentOrder.title }}</el-descriptions-item>
          <el-descriptions-item v-if="dispatchMode === 'reassign'" label="当前维修人员">
            <el-tag type="primary" size="small">{{ currentOrder.workerName || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="维修地址">{{ currentOrder.address }}</el-descriptions-item>
        </template>
      </el-descriptions>

      <!-- 维修人员选择（数据源 = D-01 负载视图，辅助均衡派单） -->
      <div class="worker-section">
        <div class="worker-section-head">
          <span class="worker-section-title">
            选择维修人员（按在途工单量由少到多排序，便于均衡派单）
          </span>
          <el-input
            v-model="workerKeyword"
            placeholder="搜索姓名 / 工种"
            clearable
            size="small"
            style="width: 200px"
          />
        </div>

        <el-table
          v-loading="workerLoading"
          :data="filteredWorkers"
          border
          height="300"
          highlight-current-row
          style="width: 100%"
          @row-click="handleWorkerRowClick"
        >
          <el-table-column width="50" align="center">
            <template #default="{ row }">
              <el-radio
                :model-value="dispatchForm.workerId"
                :value="row.workerId"
                @change="dispatchForm.workerId = row.workerId"
              >
                <span />
              </el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="workerType" label="维修工种" width="110">
            <template #default="{ row }">{{ row.workerType || '-' }}</template>
          </el-table-column>
          <el-table-column label="工作状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="workStatusTag(row.workStatus)" size="small" effect="plain">
                {{ workStatusName(row.workStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="在途工单" width="100" align="center" sortable :sort-method="(a, b) => a.pendingCount - b.pendingCount">
            <template #default="{ row }">
              <el-tag :type="Number(row.pendingCount) >= 5 ? 'danger' : 'success'" size="small">
                {{ row.pendingCount ?? 0 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="processingCount" label="处理中" width="90" align="center" />
          <el-table-column prop="finishedCount" label="累计完成" width="100" align="center" />
          <el-table-column prop="skillDesc" label="技能描述" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.skillDesc || '-' }}</template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无可用维修人员（仅在启用状态的维修人员可被派单）" />
          </template>
        </el-table>

        <div class="worker-tip">
          口径说明：<b>在途</b> = 状态 1~4 工单数；<b>处理中</b> = 状态 2~3 工单数；<b>累计完成</b> = 状态 5~6 工单数。
          账号已停用的维修人员不会出现在列表中（BR-3-03）。
        </div>
      </div>

      <!-- 备注 -->
      <el-form :model="dispatchForm" label-width="90px" style="margin-top: 12px">
        <el-form-item :label="dispatchMode === 'reassign' ? '改派说明' : '派单备注'">
          <el-input
            v-model="dispatchForm.dispatchRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            :placeholder="dispatchMode === 'reassign'
              ? '可选，说明改派原因（≤255 字），例：原维修人员临时请假，改派给同工种同事。'
              : '可选，给维修人员的说明（≤255 字），例：居民反映厨房水管接口渗水，请携带生料带与扳手。'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!dispatchForm.workerId" @click="handleDispatchSubmit">
          {{ dispatchMode === 'batch' ? '确认批量派单' : dispatchMode === 'reassign' ? '确认改派' : '确认派单' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 发布抢单池弹窗（D-05） ==================== -->
    <el-dialog v-model="publishVisible" title="发布至抢单池" width="520px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        发布后工单状态<b>仍为「待派单」</b>，但会进入维修人员的「抢单大厅」，
        可被任一维修人员抢单领取（先到先得）。重复发布会提示冲突。
      </el-alert>

      <el-form label-width="90px">
        <el-form-item label="工单编号">
          <span>{{ publishForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="报修标题">
          <span>{{ publishForm.title }}</span>
        </el-form-item>
        <el-form-item label="发布说明">
          <el-input
            v-model="publishForm.dispatchRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="可选，发布说明（≤255 字），例：适合就近维修，欢迎附近维修人员领取。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePublishSubmit">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 派单轨迹抽屉（D-10 公共组件） ==================== -->
    <DispatchRecordDrawer v-model="recordsVisible" :order-id="recordsOrderId" :order-no="recordsOrderNo" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders } from '@/api/order'
import {
  batchDispatch,
  dispatchOrder,
  getWorkerWorkload,
  pagePendingDispatch,
  publishGrabPool,
  reassignOrder
} from '@/api/dispatch'
import { getCategoryTree } from '@/api/category'
import { statusName, statusTag } from '@/constants/order'
import { workStatusName, workStatusTag } from '@/constants/dispatch'
import DispatchRecordDrawer from '@/components/DispatchRecordDrawer.vue'

/**
 * 管理员派单工作台（模块三 · D-01 ~ D-05、D-10、D-11）
 *
 * 两个标签页共用一个组件，覆盖管理员侧全部派单动作：
 *  ① 待派单池（status = 0）—— 列表走 D-11 语义化接口；
 *     操作：派单（D-02）、批量派单（D-03）、发布抢单池（D-05）、派单轨迹（D-10）。
 *  ② 已派单待接单（status = 1）—— 列表复用模块二 O-02，服务端按管理员身份不限数据范围；
 *     操作：改派（D-04，仅未接单可改）、派单轨迹（D-10）。
 *
 * 派单弹窗的选人数据源为 D-01「维修人员负载视图」，展示在途/处理中/累计完成三个指标，
 * 并按在途工单量升序，帮助管理员做均衡派单（FR-3-06）。
 *
 * 设计原则：状态合法性、角色权限、数据归属全部由服务端强制校验，
 * 前端仅在交互层做「不可能状态」的按钮隐藏与二次确认，不作为安全边界。
 */
const router = useRouter()

// ======================= 通用状态 =======================
const activeTab = ref('pending')
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])
const selection = ref([])

/** 待派单池查询条件（对齐 D-11 PendingDispatchQueryDTO） */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryId: null,
  urgency: null
})

/** 已派单待接单查询条件（对齐 O-02 OrderQueryDTO） */
const query2 = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

/** 类目筛选选项（模块一 C-02 类目树扁平化） */
const categoryOptions = ref([])

async function fetchCategoryOptions () {
  try {
    const res = await getCategoryTree(false)
    const options = []
    ;(res.data || []).forEach((root) => {
      options.push({ label: root.categoryName, value: root.categoryId })
      ;(root.children || []).forEach((child) => {
        options.push({ label: `${root.categoryName} / ${child.categoryName}`, value: child.categoryId })
      })
    })
    categoryOptions.value = options
  } catch (e) {
    categoryOptions.value = []
  }
}

/**
 * 分页查询：按当前标签页路由到不同接口。
 *  - pending：D-11 待派单池（服务端强制 status = 0，默认紧急置顶）；
 *  - dispatched：O-02 且 status = 1（已派单待接单）。
 */
async function fetchList () {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'pending') {
      res = await pagePendingDispatch({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
        urgency: query.urgency === null || query.urgency === '' ? undefined : query.urgency,
        startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
        endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
      })
    } else {
      res = await pageOrders({
        pageNum: query2.pageNum,
        pageSize: query2.pageSize,
        status: 1,
        keyword: query2.keyword || undefined
      })
    }
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
    selection.value = []
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange () {
  // 切换标签页后重置分页，避免沿用另一页的页码
  query.pageNum = 1
  query2.pageNum = 1
  fetchList()
}

function handleSearch () {
  if (activeTab.value === 'pending') {
    query.pageNum = 1
  } else {
    query2.pageNum = 1
  }
  fetchList()
}

function handleReset () {
  query.categoryId = null
  query.urgency = null
  query2.keyword = ''
  timeRange.value = []
  query.pageNum = 1
  query2.pageNum = 1
  fetchList()
}

function handleSelectionChange (rows) {
  selection.value = rows
}

function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

// ======================= 派单轨迹（D-10） =======================
const recordsVisible = ref(false)
const recordsOrderId = ref(null)
const recordsOrderNo = ref('')

function openRecords (row) {
  recordsOrderId.value = row.orderId
  recordsOrderNo.value = row.orderNo
  recordsVisible.value = true
}

// ======================= 维修人员负载视图（D-01） =======================
const workerLoading = ref(false)
const workers = ref([])
const workerKeyword = ref('')

async function fetchWorkerWorkload () {
  workerLoading.value = true
  try {
    // onlyEnabled 默认 true：停用账号不可被派单（BR-3-03）
    const res = await getWorkerWorkload({ onlyEnabled: true })
    // 按在途工单量升序，把负载最轻的人排在前面，辅助均衡派单
    workers.value = (res.data || []).slice().sort(
      (a, b) => Number(a.pendingCount || 0) - Number(b.pendingCount || 0)
    )
  } catch (e) {
    workers.value = []
  } finally {
    workerLoading.value = false
  }
}

/** 前端本地过滤：仅用于选人时的快速定位，不改变提交给服务端的人员集合 */
const filteredWorkers = computed(() => {
  const kw = workerKeyword.value.trim().toLowerCase()
  if (!kw) return workers.value
  return workers.value.filter((w) =>
    (w.realName || '').toLowerCase().includes(kw) ||
    (w.workerType || '').toLowerCase().includes(kw)
  )
})

function handleWorkerRowClick (row) {
  dispatchForm.workerId = row.workerId
}

// ======================= 派单弹窗（D-02 / D-03 / D-04 共用） =======================
const dispatchVisible = ref(false)
/** 弹窗模式：dispatch 派单 / batch 批量派单 / reassign 改派 */
const dispatchMode = ref('dispatch')
const currentOrder = ref({})
const batchRows = ref([])
const dispatchForm = reactive({ workerId: null, dispatchRemark: '' })

const dispatchTitle = computed(() => {
  if (dispatchMode.value === 'batch') return '批量派单'
  if (dispatchMode.value === 'reassign') return '改派维修人员'
  return '派单给维修人员'
})

const dialogAlertType = computed(() => (dispatchMode.value === 'reassign' ? 'warning' : 'info'))

const dialogAlertText = computed(() => {
  if (dispatchMode.value === 'batch') {
    return '批量派单在单一事务中逐单处理：状态不合法的工单不会阻断其余工单，而是在结果中列出失败明细。'
  }
  if (dispatchMode.value === 'reassign') {
    return '改派后工单状态仍为「已派单」，仅更换受派人员，并留下一条成对记录原 / 新受派人的改派轨迹。'
  }
  return '派单成功后工单状态变为「已派单（1）」并自动关联维修人员，维修人员即可在「我的工单」中接单。'
})

function resetDispatchForm () {
  dispatchForm.workerId = null
  dispatchForm.dispatchRemark = ''
  workerKeyword.value = ''
}

function openDispatch (row) {
  dispatchMode.value = 'dispatch'
  currentOrder.value = { ...row }
  batchRows.value = []
  resetDispatchForm()
  dispatchVisible.value = true
}

function openBatchDispatch () {
  if (selection.value.length === 0) {
    ElMessage.warning('请先勾选需要派单的工单')
    return
  }
  if (selection.value.length > 50) {
    ElMessage.warning('批量派单单次最多 50 条，请减少勾选数量')
    return
  }
  dispatchMode.value = 'batch'
  batchRows.value = [...selection.value]
  currentOrder.value = {}
  resetDispatchForm()
  dispatchVisible.value = true
}

function openReassign (row) {
  dispatchMode.value = 'reassign'
  currentOrder.value = { ...row }
  batchRows.value = []
  resetDispatchForm()
  dispatchVisible.value = true
}

async function handleDispatchSubmit () {
  if (!dispatchForm.workerId) {
    ElMessage.warning('请选择维修人员')
    return
  }

  // 二次确认：明确告知目标人员与工单数量，避免误操作
  const worker = workers.value.find((w) => w.workerId === dispatchForm.workerId)
  const workerName = worker ? worker.realName : '所选维修人员'
  const summary = dispatchMode.value === 'batch'
    ? `确认将 ${batchRows.value.length} 条待派单工单批量指派给「${workerName}」吗？`
    : dispatchMode.value === 'reassign'
      ? `确认将工单「${currentOrder.value.orderNo}」改派给「${workerName}」吗？`
      : `确认将工单「${currentOrder.value.orderNo}」派给「${workerName}」吗？`

  try {
    await ElMessageBox.confirm(summary, '派单确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }

  submitting.value = true
  try {
    const payload = {
      workerId: dispatchForm.workerId,
      dispatchRemark: dispatchForm.dispatchRemark.trim() || undefined
    }

    if (dispatchMode.value === 'batch') {
      const res = await batchDispatch({
        orderIds: batchRows.value.map((r) => r.orderId),
        ...payload
      })
      const data = res.data || {}
      if (data.failCount > 0) {
        // 部分成功：用弹窗列出失败明细，而非只弹一条 toast
        const failLines = (data.failList || [])
          .map((f) => `${f.orderNo || f.orderId}：${f.reason}`)
          .join('<br />')
        ElMessageBox.alert(
          `成功 ${data.successCount} 条，失败 ${data.failCount} 条。<br /><br />${failLines}`,
          '批量派单结果',
          { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
        )
      } else {
        ElMessage.success(res.message || `批量派单成功，共 ${data.successCount} 条`)
      }
    } else if (dispatchMode.value === 'reassign') {
      const res = await reassignOrder(currentOrder.value.orderId, payload)
      ElMessage.success(res.message || '改派成功')
    } else {
      const res = await dispatchOrder(currentOrder.value.orderId, payload)
      ElMessage.success(res.message || '派单成功，工单已流转为「已派单」')
    }

    dispatchVisible.value = false
    fetchList()
  } catch (e) {
    // 400 / 403 / 409 等错误提示由响应拦截器统一处理，这里不重复弹窗
  } finally {
    submitting.value = false
  }
}

// ======================= 发布抢单池（D-05） =======================
const publishVisible = ref(false)
const publishForm = reactive({ orderId: null, orderNo: '', title: '', dispatchRemark: '' })

function openPublish (row) {
  publishForm.orderId = row.orderId
  publishForm.orderNo = row.orderNo
  publishForm.title = row.title
  publishForm.dispatchRemark = ''
  publishVisible.value = true
}

async function handlePublishSubmit () {
  submitting.value = true
  try {
    const res = await publishGrabPool(publishForm.orderId, {
      dispatchRemark: publishForm.dispatchRemark.trim() || undefined
    })
    ElMessage.success(res.message || '已发布至抢单池')
    publishVisible.value = false
    fetchList()
  } catch (e) {
    // 409（重复发布）等提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await fetchCategoryOptions()
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

.worker-section {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.worker-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.worker-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.worker-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
