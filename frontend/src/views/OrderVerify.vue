<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明（按角色给出不同引导） ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        <template v-if="isAdmin">
          本页为<b>工单验收</b>：展示全部<b>待验收(4)</b>工单。维修人员提交完工后，工单由维修进度模块
          流转到此；<b>验收由居民本人操作</b>。若工单完工后超过 <b>{{ OVERDUE_HOURS }} 小时</b>
          居民仍未验收（列表中标记「验收超时」），管理员可执行<b>代验收</b>（FR-5-07 / BR-5-08）。
        </template>
        <template v-else>
          本页为<b>我的验收</b>：只展示<b>本人提交的</b>待验收工单（服务端按当前登录身份自动限定）。
          请核对维修结果后选择<b>验收通过</b>（工单转为「已验收」，即可前往评价）或<b>验收驳回</b>
          （工单退回「维修中」，由原维修人员返修）。请在完工后
          <b>{{ OVERDUE_HOURS }} 小时</b>内完成验收。
        </template>
      </el-alert>

      <!-- ============ 搜索区（V-01 查询条件） ============ -->
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

        <el-form-item label="验收时限">
          <el-switch
            v-model="query.onlyOverdue"
            active-text="仅看超时未验收"
            inline-prompt
            style="--el-switch-on-color: #e6a23c"
          />
        </el-form-item>

        <el-form-item label="完工时间">
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

      <!-- ============ 工具条 ============ -->
      <div class="toolbar">
        <el-tag type="warning" effect="dark" size="large">待验收 {{ total }} 条</el-tag>
        <span class="toolbar-tip">
          验收通过 → 工单流转为「已验收(5)」，流入服务评价；验收驳回 → 回退「维修中(3)」，原维修人员返修
        </span>
      </div>

      <!-- ============ 待验收工单列表（V-01） ============ -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
        <el-table-column prop="title" label="报修标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="报修类目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="address" label="维修地址" min-width="170" show-overflow-tooltip />

        <!-- 报修居民仅管理员可见（管理员看待验收全量，需要知道是谁的工单） -->
        <el-table-column v-if="isAdmin" label="报修居民" width="100" align="center">
          <template #default="{ row }">{{ row.residentName || '-' }}</template>
        </el-table-column>

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

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ row.statusName || statusName(row.status) }}
            </el-tag>
            <!-- FR-5-06 超时提醒：verifyOverdue 由服务端按 48 小时阈值派生 -->
            <div v-if="row.verifyOverdue" class="overdue">
              验收超时<br />{{ overdueText(row.finishTime) }}
            </div>
            <div v-else class="remain">{{ overdueText(row.finishTime) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">工单详情</el-button>
            <el-button link type="info" size="small" @click="openRepairRecords(row)">维修过程</el-button>
            <el-button link type="info" size="small" @click="openVerifyRecords(row)">验收记录</el-button>

            <!-- 居民验收：与服务端 V-02 的状态域（仅 4 待验收）保持一致 -->
            <template v-if="isResident">
              <el-button
                v-if="canVerify(row.status)"
                link
                type="success"
                size="small"
                @click="openConfirm(row, 'verify')"
              >验收通过</el-button>
              <el-button
                v-if="canVerify(row.status)"
                link
                type="danger"
                size="small"
                @click="openReject(row)"
              >驳回返修</el-button>
            </template>

            <!-- 管理员代验收：仅在已超时（与后端 BR-5-08 阈值同源）的行上展示 -->
            <el-button
              v-if="isAdmin && canVerify(row.status) && row.verifyOverdue"
              link
              type="warning"
              size="small"
              @click="openConfirm(row, 'agent')"
            >代验收</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="emptyText" />
        </template>
      </el-table>

      <!-- ============ 分页（接口文档 2.5 节） ============ -->
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

    <!-- ============ 维修过程时间轴（复用模块四公共组件，FR-5-02 查看维修结果） ============ -->
    <OrderRecordTimeline
      v-model="repairRecordsVisible"
      :order-id="currentOrder.orderId"
      :order-no="currentOrder.orderNo"
    />

    <!-- ============ 验收记录时间轴（V-04，本模块组件） ============ -->
    <VerifyRecordTimeline
      v-model="verifyRecordsVisible"
      :order-id="currentOrder.orderId"
      :order-no="currentOrder.orderNo"
    />

    <!-- ============ 验收确认弹窗（V-02 通过 / V-03 管理员代验收） ============ -->
    <VerifyConfirmDialog
      v-model="confirmVisible"
      :order="currentOrder"
      :mode="confirmMode"
      @success="fetchList"
    />

    <!-- ============ 驳回返修弹窗（V-02 驳回） ============ -->
    <VerifyRejectDialog v-model="rejectVisible" :order="currentOrder" @success="fetchList" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pagePendingVerify } from '@/api/verify'
import { getCategoryTree } from '@/api/category'
import { statusName, statusTag } from '@/constants/order'
import { OVERDUE_HOURS, canVerify, overdueText } from '@/constants/verify'
import { useUserStore } from '@/store/user'
import OrderRecordTimeline from '@/components/OrderRecordTimeline.vue'
import VerifyRecordTimeline from '@/components/VerifyRecordTimeline.vue'
import VerifyConfirmDialog from '@/components/VerifyConfirmDialog.vue'
import VerifyRejectDialog from '@/components/VerifyRejectDialog.vue'

/**
 * 工单验收（模块五：待验收工单列表页，V-01）
 *
 * 数据范围由服务端自动限定：居民只看到本人提交的待验收工单，
 * 管理员看到全部；前端不传 residentId，也无法通过参数越权查看他人数据。
 * 维修人员无权访问本页（V-01 返回 403），且路由 meta.roles 已将其隔离在菜单之外。
 *
 * 「查看维修结果」（FR-5-02）直接复用模块二的工单详情（O-03）与模块四的
 * 维修进度时间轴（R-01，OrderRecordTimeline 公共组件），本模块不重复实现。
 */
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.role === 'admin')
const isResident = computed(() => userStore.role === 'resident')

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryId: null,
  onlyOverdue: false
})

const emptyText = computed(() =>
  query.onlyOverdue
    ? `暂无超时未验收的工单（完工未超 ${OVERDUE_HOURS} 小时的工单不在此列表）`
    : '暂无待验收工单：维修人员提交完工后，工单会出现在这里'
)

// ======================= 类目筛选选项（复用模块一 C-02 类目树） =======================
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

// ======================= 列表查询（V-01） =======================
async function fetchList () {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
      onlyOverdue: query.onlyOverdue ? true : undefined,
      // 本接口的时间区间口径是「完工时间」，与「我的报修」的提交时间不同
      startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
      endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
    }
    const res = await pagePendingVerify(params)
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
  query.categoryId = null
  query.onlyOverdue = false
  timeRange.value = []
  query.pageNum = 1
  fetchList()
}

// ======================= 页面跳转与抽屉 =======================
/** 当前操作行：弹窗与抽屉共用一个「当前工单」，避免为每个弹窗各存一份状态 */
const currentOrder = reactive({ orderId: null, orderNo: '' })

const repairRecordsVisible = ref(false)
const verifyRecordsVisible = ref(false)

function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

/** 查看维修结果：复用模块四的维修进度时间轴（R-01，含维修后照片与完工说明） */
function openRepairRecords (row) {
  Object.assign(currentOrder, { orderId: row.orderId, orderNo: row.orderNo })
  repairRecordsVisible.value = true
}

/** 查看验收记录：本模块 V-04，多轮验收留痕 */
function openVerifyRecords (row) {
  Object.assign(currentOrder, { orderId: row.orderId, orderNo: row.orderNo })
  verifyRecordsVisible.value = true
}

// ======================= 验收 / 驳回 / 代验收 =======================
const confirmVisible = ref(false)
const rejectVisible = ref(false)
/** 'verify' 居民验收 / 'agent' 管理员代验收，透传给同一弹窗组件 */
const confirmMode = ref('verify')

function openConfirm (row, mode) {
  Object.assign(currentOrder, { ...row, orderId: row.orderId, orderNo: row.orderNo })
  confirmMode.value = mode
  confirmVisible.value = true
}

function openReject (row) {
  Object.assign(currentOrder, { ...row, orderId: row.orderId, orderNo: row.orderNo })
  rejectVisible.value = true
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

.overdue {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.4;
  color: #e6a23c;
}

.remain {
  margin-top: 2px;
  font-size: 12px;
  color: #67c23a;
}
</style>
