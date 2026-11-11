<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 页面说明 ============ -->
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页为维修人员<b>工单大厅</b>：可在「抢单大厅」自由领取管理员发布至抢单池的待派单工单（先到先得）；
        领取或被指派后，工单出现在「我的待接单」中，可<b>接单</b>确认上门（流转为「已接单（2）」），
        或说明原因后<b>拒单</b>退回待派单池（状态回退为 0）。
      </el-alert>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================== 标签一：抢单大厅（D-06 / D-07） ==================== -->
        <el-tab-pane label="抢单大厅" name="pool">
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

            <el-form-item label="维修地址">
              <el-input
                v-model="query.addressKeyword"
                placeholder="按地址模糊搜索"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
              <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="toolbar">
            <el-tag type="success" effect="dark" size="large">可抢工单 {{ total }} 条</el-tag>
            <span class="toolbar-tip">
              抢单为<b>先到先得</b>：同一工单被多人同时抢时，仅一人成功，其余提示工单已被领取（409）
            </span>
          </div>

          <el-table v-loading="loading" :data="tableData" style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="报修类目" min-width="150" show-overflow-tooltip />
            <el-table-column prop="address" label="维修地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="紧急" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
                <span v-else class="sub-text">普通</span>
              </template>
            </el-table-column>
            <el-table-column label="期望上门" width="160" align="center">
              <template #default="{ row }">{{ row.expectTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetailDialog(row)">查看详情</el-button>
                <el-button link type="success" size="small" @click="handleGrab(row)">抢单</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无可抢工单，请留意管理员发布的抢单池" />
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

        <!-- ==================== 标签二：我的待接单（D-08 / D-09 / D-10） ==================== -->
        <el-tab-pane label="我的待接单" name="mine">
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
            <el-tag type="warning" effect="dark" size="large">待接单 {{ total }} 条</el-tag>
            <span class="toolbar-tip">
              接单后工单进入「已接单（2）」，可在下游「维修进度管理」中反馈维修进度；
              拒单须填写 5~200 字原因，工单将退回待派单池并由管理员重新派单
            </span>
          </div>

          <el-table v-loading="loading" :data="tableData" style="width: 100%">
            <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
            <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="报修类目" min-width="150" show-overflow-tooltip />
            <el-table-column prop="address" label="维修地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="联系人 / 电话" width="140" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.contactName || '-' }}<br />
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
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
                <el-button link type="success" size="small" @click="handleAccept(row)">接单</el-button>
                <el-button link type="danger" size="small" @click="openReject(row)">拒单</el-button>
                <el-button link type="info" size="small" @click="openRecords(row)">派单轨迹</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无待接单工单，可在「抢单大厅」领取或等待管理员派单" />
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

    <!-- ==================== 抢单池工单详情弹窗（D-06 元素只读展示） ==================== -->
    <el-dialog v-model="detailVisible" title="工单详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="工单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="报修类目">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="报修标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="维修地址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag v-if="detail.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
          <span v-else class="sub-text">普通</span>
        </el-descriptions-item>
        <el-descriptions-item label="期望上门">{{ detail.expectTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="warning" size="small" effect="dark">待派单（抢单池）</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">
          <div class="desc-text">{{ detail.description || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.images" label="现场照片" :span="2">
          <div class="image-row">
            <el-image
              v-for="(img, idx) in imageList(detail.images)"
              :key="idx"
              :src="img"
              :preview-src-list="imageList(detail.images)"
              :initial-index="idx"
              fit="cover"
              class="preview-img"
              preview-teleported
            />
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handleGrabFromDetail">抢单</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 接单弹窗（D-08，备注可选） ==================== -->
    <el-dialog v-model="acceptVisible" title="接单确认" width="520px">
      <el-alert type="success" :closable="false" show-icon style="margin-bottom: 12px">
        接单后工单状态变为「已接单（2）」，并自动生成维修进度时间线的首个节点。
        请在居民期望时间上门，并在「维修进度管理」中及时反馈进展。
      </el-alert>

      <el-form label-width="90px">
        <el-form-item label="工单编号">
          <span>{{ acceptForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="报修标题">
          <span>{{ acceptForm.title }}</span>
        </el-form-item>
        <el-form-item label="上门地址">
          <span>{{ acceptForm.address }}</span>
        </el-form-item>
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

    <!-- ==================== 拒单弹窗（D-09，原因必填 5~200 字） ==================== -->
    <el-dialog
      v-model="rejectVisible"
      title="拒单"
      width="520px"
      @closed="rejectRef?.clearValidate()"
    >
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        拒单后工单将退回<b>待派单池</b>并清空当前受派人信息，同时生成一条「转派退回」记录，
        由管理员重新派单。请如实填写拒单原因，便于管理员安排合适的维修人员。
      </el-alert>

      <el-form ref="rejectRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="工单编号">
          <span>{{ rejectForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="报修标题">
          <span>{{ rejectForm.title }}</span>
        </el-form-item>
        <el-form-item label="拒单原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写拒单原因（5~200 字），例：本人非水电工种，无法处理水管接口渗水，建议改派水暖工。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleRejectSubmit">确认拒单</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 派单轨迹抽屉（D-10 公共组件） ==================== -->
    <DispatchRecordDrawer v-model="recordsVisible" :order-id="recordsOrderId" :order-no="recordsOrderNo" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders } from '@/api/order'
import { acceptOrder, grabOrder, pageGrabPool, rejectOrder } from '@/api/dispatch'
import { getCategoryTree } from '@/api/category'
import { statusName, statusTag } from '@/constants/order'
import { REJECT_ORDER_REASON_MAX, REJECT_ORDER_REASON_MIN } from '@/constants/dispatch'
import DispatchRecordDrawer from '@/components/DispatchRecordDrawer.vue'

/**
 * 维修人员工单大厅（模块三 · D-06 ~ D-10）
 *
 * 两个标签页：
 *  ① 抢单大厅（D-06）—— 已发布至抢单池且尚未被领取的待派单工单；操作：抢单（D-07）。
 *  ② 我的待接单（状态 1）—— 列表复用模块二 O-02，服务端按维修人员角色
 *     自动限定为「本人受派工单」（worker_id = 当前用户）；操作：接单（D-08）、拒单（D-09）、轨迹（D-10）。
 *
 * 注意：D-08 / D-09 除角色校验外，服务端还要求请求者必须是该工单的受派人，
 * 因此本页只展示本人受派工单，恶意构造请求也会被服务端拒绝（403）。
 */
const router = useRouter()

const activeTab = ref('pool')
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryId: null,
  urgency: null,
  addressKeyword: ''
})

const query2 = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

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
    let res
    if (activeTab.value === 'pool') {
      // D-06 抢单池列表
      res = await pageGrabPool({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
        urgency: query.urgency === null || query.urgency === '' ? undefined : query.urgency,
        addressKeyword: query.addressKeyword || undefined
      })
    } else {
      // 我的待接单：O-02 且 status = 1，服务端自动限定为本人受派工单
      res = await pageOrders({
        pageNum: query2.pageNum,
        pageSize: query2.pageSize,
        status: 1,
        keyword: query2.keyword || undefined
      })
    }
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
  query2.pageNum = 1
  fetchList()
}

function handleSearch () {
  if (activeTab.value === 'pool') {
    query.pageNum = 1
  } else {
    query2.pageNum = 1
  }
  fetchList()
}

function handleReset () {
  query.categoryId = null
  query.urgency = null
  query.addressKeyword = ''
  query2.keyword = ''
  query.pageNum = 1
  query2.pageNum = 1
  fetchList()
}

function goDetail (row) {
  router.push(`/orders/${row.orderId}`)
}

/** 抢单池 images 字段为逗号分隔路径，拆分为图片数组供 el-image 预览 */
function imageList (images) {
  if (!images) return []
  return String(images).split(',').map((s) => s.trim()).filter(Boolean)
}

// ======================= 抢单池详情弹窗 =======================
const detailVisible = ref(false)
const detail = ref({})

function openDetailDialog (row) {
  detail.value = { ...row }
  detailVisible.value = true
}

// ======================= 抢单（D-07） =======================
async function handleGrab (row) {
  try {
    await ElMessageBox.confirm(
      `确认抢下工单「${row.orderNo}」吗？抢单成功后该工单将指派给你，需及时接单并上门维修。`,
      '抢单确认',
      { confirmButtonText: '确认抢单', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }
  await doGrab(row.orderId)
}

async function handleGrabFromDetail () {
  if (!detail.value.orderId) return
  try {
    await ElMessageBox.confirm(
      `确认抢下工单「${detail.value.orderNo}」吗？`,
      '抢单确认',
      { confirmButtonText: '确认抢单', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  await doGrab(detail.value.orderId)
}

async function doGrab (orderId) {
  submitting.value = true
  try {
    const res = await grabOrder(orderId)
    ElMessage.success(res.message || '抢单成功，工单已指派给你，请及时接单')
    detailVisible.value = false
    fetchList()
  } catch (e) {
    // 409「该工单已被其他维修人员领取」等提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ======================= 接单（D-08） =======================
const acceptVisible = ref(false)
const acceptForm = reactive({ orderId: null, orderNo: '', title: '', address: '', remark: '' })

function handleAccept (row) {
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
    const res = await acceptOrder(acceptForm.orderId, {
      remark: acceptForm.remark.trim() || undefined
    })
    ElMessage.success(res.message || '接单成功，工单已进入「已接单」')
    acceptVisible.value = false
    fetchList()
  } catch (e) {
    // 权限 / 状态冲突提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ======================= 拒单（D-09） =======================
const rejectVisible = ref(false)
const rejectRef = ref(null)
const rejectForm = reactive({ orderId: null, orderNo: '', title: '', rejectReason: '' })

const rejectRules = {
  rejectReason: [
    { required: true, message: '请填写拒单原因', trigger: 'blur' },
    {
      min: REJECT_ORDER_REASON_MIN,
      max: REJECT_ORDER_REASON_MAX,
      message: `拒单原因长度须为 ${REJECT_ORDER_REASON_MIN}~${REJECT_ORDER_REASON_MAX} 个字符`,
      trigger: 'blur'
    }
  ]
}

function openReject (row) {
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
    const res = await rejectOrder(rejectForm.orderId, {
      rejectReason: rejectForm.rejectReason.trim()
    })
    ElMessage.success(res.message || '已拒单，工单将退回待派单池')
    rejectVisible.value = false
    fetchList()
  } catch (e) {
    // 400 / 403 / 409 提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
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

.desc-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.image-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
