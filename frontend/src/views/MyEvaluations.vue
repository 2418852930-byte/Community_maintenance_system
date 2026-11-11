<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明（按角色给出不同引导） ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        <template v-if="isRepair">
          本页为<b>我的评价</b>：展示<b>本人所接工单</b>收到的服务评价（服务端按当前登录身份自动限定，
          无法查看他人评价）。依据 BR-6-06，您<b>只能看到星级与评价内容</b>；
          若居民选择匿名评价，评价人将显示为「匿名用户」。
        </template>
        <template v-else>
          本页为<b>服务评价</b>：<b>「待评价」</b>列出本人已验收(5)且尚未评价的工单，
          点击「去评价」填写三项星级评分（服务态度 / 响应速度 / 维修质量）与文字评价；
          提交后工单流转为<b>已完成(6)</b>，业务链路闭环结束（BR-6-04）。
          依据 BR-6-02，<b>一个工单只能评价一次</b>，且提交后不可修改或删除（BR-6-05）。
        </template>
      </el-alert>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================================================================
             Tab ① 待评价工单（仅居民；FR-6-01 由模块二 O-02 + status=5 承载）
             ================================================================== -->
        <el-tab-pane v-if="!isRepair" name="pending">
          <template #label>
            <span>待评价</span>
            <el-badge v-if="pendingTotal > 0" :value="pendingTotal" class="tab-badge" />
          </template>

          <el-table v-loading="pendingLoading" :data="pendingList" style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="title" label="报修标题" min-width="140" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="报修类目" min-width="140" show-overflow-tooltip />
            <el-table-column label="维修人员" width="100" align="center">
              <template #default="{ row }">{{ row.workerName || '-' }}</template>
            </el-table-column>
            <el-table-column label="完工时间" width="160" align="center">
              <template #default="{ row }">{{ row.finishTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="维修费用" width="110" align="right">
              <template #default="{ row }">
                <span class="cost">¥ {{ Number(row.totalCost || 0).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small" effect="dark">
                  {{ row.statusName || statusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goDetail(row)">工单详情</el-button>
                <el-button link type="success" size="small" @click="openSubmit(row)">去评价</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无待评价工单：工单验收通过后，会出现在这里（状态「已验收」）" />
            </template>
          </el-table>

          <el-pagination
            v-model:current-page="pendingQuery.pageNum"
            v-model:page-size="pendingQuery.pageSize"
            :total="pendingTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="fetchPending"
            @current-change="fetchPending"
          />
        </el-tab-pane>

        <!-- ==================================================================
             Tab ② 评价列表（居民=我的评价 FR-6-04；维修人员=收到的评价 FR-6-07）
             ================================================================== -->
        <el-tab-pane :label="isRepair ? '收到的评价' : '我的评价'" name="list">
          <!-- 搜索区（E-02 筛选条件；workerId / categoryId 为管理员专属，此处不提供） -->
          <el-form :model="query" inline>
            <el-form-item label="综合评分">
              <el-select v-model="query.totalScore" placeholder="全部评分" clearable style="width: 170px">
                <el-option
                  v-for="item in SCORE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="评价时间">
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

          <div class="toolbar">
            <el-tag type="success" effect="dark" size="large">
              {{ isRepair ? '收到的评价' : '我的评价' }} {{ total }} 条
            </el-tag>
            <span class="toolbar-tip">
              评价一经提交即不可修改或删除（BR-6-05）
            </span>
          </div>

          <el-table v-loading="loading" :data="tableData" style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="categoryName" label="报修类目" min-width="140" show-overflow-tooltip />

            <!-- 居民看「被评价的维修人员」；维修人员看「评价人」（匿名时为「匿名用户」） -->
            <el-table-column v-if="isRepair" label="评价人" width="110" align="center">
              <template #default="{ row }">
                {{ row.residentName || '-' }}
                <el-tag v-if="row.isAnonymous === 1" type="info" size="small" effect="plain">匿名</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-else label="维修人员" width="110" align="center">
              <template #default="{ row }">{{ row.workerName || '-' }}</template>
            </el-table-column>

            <el-table-column label="综合评分" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="scoreTag(row.totalScore)" size="small" effect="dark">
                  {{ row.totalScore }} 星
                </el-tag>
                <span class="score-text">{{ scoreText(row.totalScore) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="评价内容" min-width="200">
              <template #default="{ row }">
                <span v-if="row.content" class="content-text">{{ row.content }}</span>
                <span v-else class="empty-text">（未填写文字评价）</span>
              </template>
            </el-table-column>

            <el-table-column label="管理员回复" min-width="180">
              <template #default="{ row }">
                <span v-if="row.replyContent" class="content-text">{{ row.replyContent }}</span>
                <el-tag v-else type="info" size="small" effect="plain">暂未回复</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="评价时间" width="160" align="center">
              <template #default="{ row }">{{ row.createTime || '-' }}</template>
            </el-table-column>

            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
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
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============ 评价提交弹窗（E-01，仅居民使用） ============ -->
    <EvaluationSubmitDialog
      v-model="submitVisible"
      :order="currentOrder"
      @success="handleEvaluateSuccess"
    />

    <!-- ============ 评价详情弹窗（E-03，全角色可用） ============ -->
    <EvaluationDetailDialog
      v-model="detailVisible"
      :evaluation-id="currentEvaluationId"
      :order-no="currentEvaluationOrderNo"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders } from '@/api/order'
import { pageEvaluations } from '@/api/evaluation'
import { statusName, statusTag } from '@/constants/order'
import { SCORE_OPTIONS, scoreTag, scoreText } from '@/constants/evaluation'
import { useUserStore } from '@/store/user'
import EvaluationSubmitDialog from '@/components/EvaluationSubmitDialog.vue'
import EvaluationDetailDialog from '@/components/EvaluationDetailDialog.vue'

/**
 * 服务评价（模块六：我的评价页面）
 *
 * 角色自适应：
 *  - 居民：Tab①「待评价」（FR-6-01，数据来自模块二 O-02 + status=5）
 *         + Tab②「我的评价」（FR-6-04，数据来自 E-02，服务端限定 resident_id = 本人）；
 *  - 维修人员：仅 Tab②「收到的评价」（FR-6-07，服务端限定 worker_id = 本人）。
 *
 * 数据范围完全由服务端强制注入，前端不传 residentId / workerId，
 * 也无法通过任何参数越权查看他人数据（NFR-S-03）。
 */
const router = useRouter()
const userStore = useUserStore()

const isRepair = computed(() => userStore.role === 'repair')

const activeTab = ref(isRepair.value ? 'list' : 'pending')

// ======================= Tab ① 待评价工单（居民） =======================
const pendingLoading = ref(false)
const pendingList = ref([])
const pendingTotal = ref(0)

const pendingQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

async function fetchPending () {
  if (isRepair.value) return
  pendingLoading.value = true
  try {
    // FR-6-01「待评价工单列表」= 模块二 O-02 + status=5（接口文档 4.7 节开头明确约定）
    const res = await pageOrders({
      pageNum: pendingQuery.pageNum,
      pageSize: pendingQuery.pageSize,
      status: 5
    })
    pendingList.value = res.data.list || []
    pendingTotal.value = res.data.total || 0
  } catch (e) {
    pendingList.value = []
    pendingTotal.value = 0
  } finally {
    pendingLoading.value = false
  }
}

// ======================= Tab ② 评价列表（全角色） =======================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  totalScore: null
})

const emptyText = computed(() =>
  isRepair.value
    ? '暂无收到的评价：居民完成评价后，评价结果会出现在这里'
    : '暂无评价记录：工单验收通过后可前往「待评价」提交评价'
)

async function fetchList () {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      totalScore: query.totalScore === null || query.totalScore === '' ? undefined : query.totalScore,
      startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
      endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
    }
    const res = await pageEvaluations(params)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch () {
  query.pageNum = 1
  fetchList()
}

function handleReset () {
  query.totalScore = null
  timeRange.value = []
  query.pageNum = 1
  fetchList()
}

function handleTabChange (name) {
  if (name === 'list') {
    fetchList()
  } else {
    fetchPending()
  }
}

// ======================= 页面跳转与弹窗 =======================
function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

/** 当前提交评价的目标工单 */
const currentOrder = reactive({})
const submitVisible = ref(false)

function openSubmit (row) {
  Object.assign(currentOrder, row)
  submitVisible.value = true
}

/**
 * 评价提交成功：先切回「待评价」Tab 并刷新（该工单已流转为 6 已完成，会自动从列表中消失），
 * 再刷新「我的评价」数据，保证两处数据一致。
 */
async function handleEvaluateSuccess () {
  await fetchPending()
  if (activeTab.value === 'list') {
    await fetchList()
  }
}

/** 当前查看详情的评价 */
const currentEvaluationId = ref(null)
const currentEvaluationOrderNo = ref('')
const detailVisible = ref(false)

function openDetail (row) {
  currentEvaluationId.value = row.evaluationId
  currentEvaluationOrderNo.value = row.orderNo || ''
  detailVisible.value = true
}

onMounted(() => {
  if (isRepair.value) {
    fetchList()
  } else {
    fetchPending()
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-tip {
  font-size: 12px;
  color: #909399;
}

.cost {
  color: #e6a23c;
  font-weight: 600;
}

.score-text {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.5;
}

.empty-text {
  color: #c0c4cc;
  font-size: 13px;
}

.tab-badge {
  margin-left: 6px;
  vertical-align: middle;
}
</style>
