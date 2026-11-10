<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 搜索区（O-02 查询条件） ============ -->
      <el-form :model="query" inline>
        <el-form-item label="工单状态">
          <el-select
            v-model="query.statusList"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部状态"
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="item in STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

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

        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="工单编号 / 标题 / 地址 / 联系人"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
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

      <!-- ============ 工具条 ============ -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="goSubmit">提交报修</el-button>
        <span class="toolbar-tip">
          提交后状态为「待审核」，管理员审核通过后进入派单流程；全流程状态可在下方实时查看
        </span>
      </div>

      <!-- ============ 工单列表（O-02） ============ -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
        <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="报修类目" min-width="150" show-overflow-tooltip />
        <el-table-column prop="address" label="维修地址" min-width="170" show-overflow-tooltip />
        <el-table-column label="联系人" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.contactName }}<br />
            <span class="sub-text">{{ row.contactPhone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="紧急" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
            <span v-else class="sub-text">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip :content="STATUS_HINT[row.status] || ''" placement="top">
              <el-tag :type="statusTag(row.status)" size="small" effect="dark">
                {{ row.statusName || statusName(row.status) }}
              </el-tag>
            </el-tooltip>
            <div v-if="row.verifyOverdue" class="overdue">验收超时</div>
          </template>
        </el-table-column>
        <el-table-column label="维修人员" width="100" align="center">
          <template #default="{ row }">
            {{ row.workerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <!-- 可修改：状态 8 待审核 / 0 待派单（与服务端 O-04 状态域一致） -->
            <el-button
              v-if="row.status === 8 || row.status === 0"
              link
              type="primary"
              size="small"
              @click="goModify(row, 'edit')"
            >
              修改
            </el-button>
            <!-- 可重新提交：状态 9 已驳回（与服务端 O-06 状态域一致） -->
            <el-button
              v-if="row.status === 9"
              link
              type="warning"
              size="small"
              @click="goModify(row, 'resubmit')"
            >
              重新提交
            </el-button>
            <!-- 可撤销：状态 8 / 0 / 9（与服务端 O-05 状态域一致，BR-2-07） -->
            <el-button
              v-if="row.status === 8 || row.status === 0 || row.status === 9"
              link
              type="danger"
              size="small"
              @click="openCancelDialog(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无报修工单，点击右上角「提交报修」开始报修" />
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

    <!-- ============ 撤销工单弹窗（O-05） ============ -->
    <el-dialog v-model="cancelVisible" title="撤销工单" width="480px" @closed="cancelRef?.clearValidate()">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        撤销后工单状态将变为「已撤销」，不可再流转，请谨慎操作。
      </el-alert>
      <el-form ref="cancelRef" :model="cancelForm" :rules="cancelRules" label-width="90px">
        <el-form-item label="工单编号">
          <span>{{ cancelForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="撤销原因" prop="cancelReason">
          <el-input
            v-model="cancelForm.cancelReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写撤销原因（5~200 字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">取消</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="handleCancelSubmit">确认撤销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { cancelOrder, pageOrders } from '@/api/order'
import { getCategoryTree } from '@/api/category'
import { STATUS_HINT, STATUS_OPTIONS, statusName, statusTag } from '@/constants/order'

/**
 * 我的报修（工单列表页，模块二 O-02）
 *
 * 数据范围由服务端自动限定为「当前登录居民本人提交的工单」（需求文档 3.3），
 * 前端不传 residentId，也无法通过参数越权查看他人数据。
 *
 * 状态列覆盖 0~9 全部 10 态，且每次查询都实时读库，
 * 下游派单调度 / 维修进度 / 验收 / 评价模块更新状态后本页立即反映。
 */
const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  statusList: [],
  categoryId: null,
  urgency: null,
  keyword: ''
})

/** 类目筛选选项：由模块一 C-02 类目树扁平化而来，二级类目显示为「父 / 子」 */
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

async function fetchList () {
  loading.value = true
  try {
    // 过滤空值，避免把空字符串传给后端
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      // 状态多值按接口约定以英文逗号分隔
      status: query.statusList && query.statusList.length ? query.statusList.join(',') : undefined,
      categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
      urgency: query.urgency === null || query.urgency === '' ? undefined : query.urgency,
      keyword: query.keyword || undefined,
      startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
      endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
    }
    const res = await pageOrders(params)
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
  query.statusList = []
  query.categoryId = null
  query.urgency = null
  query.keyword = ''
  timeRange.value = []
  query.pageNum = 1
  fetchList()
}

// ======================= 页面跳转 =======================
function goSubmit () {
  router.push('/orders/submit')
}

function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

/** @param {'edit'|'resubmit'} mode */
function goModify (row, mode) {
  router.push({ path: '/orders/submit', query: { orderId: row.orderId, mode } })
}

// ======================= 撤销工单（O-05） =======================
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelRef = ref(null)
const cancelForm = reactive({ orderId: null, orderNo: '', cancelReason: '' })

const cancelRules = {
  cancelReason: [
    { required: true, message: '请填写撤销原因', trigger: 'blur' },
    { min: 5, max: 200, message: '撤销原因长度须为 5~200 个字符', trigger: 'blur' }
  ]
}

function openCancelDialog (row) {
  cancelForm.orderId = row.orderId
  cancelForm.orderNo = row.orderNo
  cancelForm.cancelReason = ''
  cancelVisible.value = true
}

async function handleCancelSubmit () {
  if (!cancelRef.value) return
  const valid = await cancelRef.value.validate().catch(() => false)
  if (!valid) return

  cancelLoading.value = true
  try {
    const res = await cancelOrder(cancelForm.orderId, cancelForm.cancelReason.trim())
    ElMessage.success(res.message || '撤销成功')
    cancelVisible.value = false
    fetchList()
  } catch (e) {
    // 409 状态不允许撤销等提示由响应拦截器统一处理
  } finally {
    cancelLoading.value = false
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
  margin-bottom: 12px;
}

.toolbar-tip {
  font-size: 12px;
  color: #909399;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}

.overdue {
  margin-top: 2px;
  font-size: 12px;
  color: #e6a23c;
}
</style>
