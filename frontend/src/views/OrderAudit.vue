<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明 ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页为管理员<b>审核工作台</b>：工单提交后状态为「待审核（8）」，在此审核通过后进入待派单池（状态 0），
        再前往「派单调度」页派单；审核驳回后状态变为「已驳回（9）」，居民将在「我的报修」中看到驳回原因并可修改后重新提交。
      </el-alert>

      <!-- ============ 搜索区（复用 O-02 查询条件，状态固定为 8） ============ -->
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
        <el-tag type="danger" effect="dark" size="large">待审核 {{ total }} 条</el-tag>
        <span class="toolbar-tip">
          审核环节不写入派单记录，仅做状态流转：通过 → 待派单(0)，驳回 → 已驳回(9)
        </span>
      </div>

      <!-- ============ 待审核工单列表（复用 O-02，status 固定 8） ============ -->
      <el-table v-loading="loading" :data="tableData" style="width: 100%">
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
          <template #default="{ row }">
            {{ row.expectTime || '-' }}
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
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="success" size="small" @click="handleAuditPass(row)">审核通过</el-button>
            <el-button link type="danger" size="small" @click="openRejectDialog(row)">驳回</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无待审核工单，居民提交报修后会出现在此处" />
        </template>
      </el-table>

      <!-- ============ 分页 ============ -->
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

    <!-- ============ 审核驳回弹窗（D-12，auditResult = 0） ============ -->
    <el-dialog
      v-model="rejectVisible"
      title="审核驳回"
      width="520px"
      @closed="rejectRef?.clearValidate()"
    >
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        驳回后工单状态变为「已驳回」，居民将在「我的报修」中看到驳回原因，
        需居民修改内容后重新提交才能再次进入审核队列。
      </el-alert>

      <el-form ref="rejectRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="工单编号">
          <span>{{ rejectForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="报修标题">
          <span>{{ rejectForm.title }}</span>
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写驳回原因/审核意见（5~200 字），例：报修描述过于简略，请补充漏水位置与故障现象，并上传现场照片。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleRejectSubmit">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders } from '@/api/order'
import { auditOrder } from '@/api/dispatch'
import { getCategoryTree } from '@/api/category'
import { statusName, statusTag } from '@/constants/order'
import { AUDIT_RESULT, REJECT_REASON_MAX, REJECT_REASON_MIN } from '@/constants/dispatch'

/**
 * 待审核工单列表（管理员 · 模块三审核工作台）
 *
 * 对应接口：
 *  - 列表：复用模块二 O-02「工单分页列表」，服务端固定 status = 8
 *    （接口文档 Q11 已确认「待审核池」不单独建接口，接口总数保持 62 个）；
 *  - 审核：D-12「工单审核（通过 / 驳回）」PUT /api/orders/{orderId}/audit。
 *
 * 设计约定：列表数据范围、状态校验、审核结果取值域全部以服务端为准，
 * 前端只负责交互与提示，不重复实现业务规则。
 */
const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
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
    const res = await pageOrders({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      // 审核工作台：状态锁定为 8 待审核，前端不可更改
      status: 8,
      categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
      urgency: query.urgency === null || query.urgency === '' ? undefined : query.urgency,
      keyword: query.keyword || undefined,
      startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
      endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
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

function handleSearch () {
  query.pageNum = 1
  fetchList()
}

function handleReset () {
  query.categoryId = null
  query.urgency = null
  query.keyword = ''
  timeRange.value = []
  query.pageNum = 1
  fetchList()
}

/** 查看工单详情（O-03，管理员可查看任意工单） */
function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

// ======================= 审核通过（D-12，auditResult = 1） =======================
const submitting = ref(false)

async function handleAuditPass (row) {
  try {
    await ElMessageBox.confirm(
      `确认审核通过工单「${row.orderNo}」吗？通过后工单将进入待派单池，等待派单。`,
      '审核通过确认',
      { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'info' }
    )
  } catch (e) {
    return // 用户取消
  }

  submitting.value = true
  try {
    const res = await auditOrder(row.orderId, { auditResult: AUDIT_RESULT[1] === '审核通过' ? 1 : 1 })
    ElMessage.success(res.message || '审核通过，工单已进入待派单池')
    fetchList()
  } catch (e) {
    // 409（状态已变化）等提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ======================= 审核驳回（D-12，auditResult = 0） =======================
const rejectVisible = ref(false)
const rejectRef = ref(null)
const rejectForm = reactive({ orderId: null, orderNo: '', title: '', rejectReason: '' })

const rejectRules = {
  rejectReason: [
    { required: true, message: '请填写驳回原因', trigger: 'blur' },
    {
      min: REJECT_REASON_MIN,
      max: REJECT_REASON_MAX,
      message: `驳回原因长度须为 ${REJECT_REASON_MIN}~${REJECT_REASON_MAX} 个字符`,
      trigger: 'blur'
    }
  ]
}

function openRejectDialog (row) {
  rejectForm.orderId = row.orderId
  rejectForm.orderNo = row.orderNo
  rejectForm.title = row.title
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

async function handleRejectSubmit () {
  if (!rejectRef.value) return
  const valid = await rejectRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await auditOrder(rejectForm.orderId, {
      auditResult: 0,
      rejectReason: rejectForm.rejectReason.trim()
    })
    ElMessage.success(res.message || '审核驳回成功')
    rejectVisible.value = false
    fetchList()
  } catch (e) {
    // 400 / 409 提示由响应拦截器统一处理
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
</style>
