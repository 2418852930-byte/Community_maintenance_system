<template>
  <div class="page-container" v-loading="loading">
    <!-- ============ 顶部：编号 + 状态 + 操作 ============ -->
    <div class="page-card head-card">
      <div class="head-left">
        <span class="order-no">{{ detail.orderNo || '-' }}</span>
        <el-tag :type="statusTag(detail.status)" effect="dark" size="large">
          {{ detail.statusName || statusName(detail.status) }}
        </el-tag>
        <el-tag v-if="detail.urgency === 1" type="danger" size="small" effect="plain">紧急</el-tag>
        <el-tag v-if="detail.verifyOverdue" type="warning" size="small" effect="plain">验收超时</el-tag>
      </div>
      <div class="head-right">
        <!-- 操作按钮严格以服务端返回的 canOperate 为准，前端不再自行判断角色与状态 -->
        <el-button v-if="canOperate.canModify" type="primary" :icon="Edit" @click="goModify('edit')">
          修改工单
        </el-button>
        <el-button
          v-if="canOperate.canResubmit"
          type="warning"
          :icon="RefreshRight"
          @click="goModify('resubmit')"
        >
          修改后重新提交
        </el-button>
        <el-button v-if="canOperate.canCancel" type="danger" plain :icon="CircleClose" @click="cancelVisible = true">
          撤销工单
        </el-button>
        <el-button :icon="Back" @click="goBack">返回</el-button>
      </div>
    </div>

    <!-- ============ 驳回提示（FR-2-05：状态 9 时展示驳回原因/驳回人/驳回时间） ============ -->
    <el-alert
      v-if="detail.status === 9"
      type="error"
      :closable="false"
      show-icon
      class="block-gap"
    >
      <template #title>该工单已被管理员审核驳回</template>
      <div class="alert-line"><b>驳回原因：</b>{{ detail.rejectReason || '（未填写）' }}</div>
      <div class="alert-line">
        驳回人：{{ detail.rejectorName || '-' }}　驳回时间：{{ detail.rejectTime || '-' }}
      </div>
      <div class="alert-line">
        请在下方「故障信息」中根据驳回意见修改内容，然后点击右上角「修改后重新提交」。
      </div>
    </el-alert>

    <!-- ============ 撤销提示 ============ -->
    <el-alert v-if="detail.status === 7" type="info" :closable="false" show-icon class="block-gap">
      <template #title>该工单已撤销，不可再流转</template>
      <div class="alert-line"><b>撤销原因：</b>{{ detail.cancelReason || '-' }}</div>
    </el-alert>

    <!-- ============ 全流程状态时间轴（任务要求之四：工单列表可查看全流程状态） ============ -->
    <div class="page-card block-gap">
      <div class="card-title">工单流转状态</div>
      <el-steps :active="flowActive" align-center finish-status="success" :process-status="flowProcessStatus">
        <el-step v-for="step in flowSteps" :key="step.code" :title="step.name" />
      </el-steps>
      <div class="flow-hint">
        {{ STATUS_HINT[detail.status] || '' }}
        <span v-if="detail.status === 9 || detail.status === 7" class="flow-extra">
          （该状态为旁支终态，不在此主链上）
        </span>
      </div>
    </div>

    <!-- ============ 基本信息 ============ -->
    <div class="page-card block-gap">
      <div class="card-title">基本信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          {{ detail.statusName || statusName(detail.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="报修标题">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修类目">{{ detail.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ detail.urgencyName || urgencyName(detail.urgency) }}</el-descriptions-item>
        <el-descriptions-item label="期望上门时间">{{ detail.expectTime || '未指定' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- ============ 报修人信息 ============ -->
    <div class="page-card block-gap">
      <div class="card-title">报修人信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修居民">{{ detail.residentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上门维修地址">{{ detail.address || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- ============ 故障信息 ============ -->
    <div class="page-card block-gap">
      <div class="card-title">故障信息</div>
      <div class="desc-block">
        <div class="desc-label">故障描述</div>
        <div class="desc-text">{{ detail.description || '-' }}</div>
      </div>
      <div class="desc-block">
        <div class="desc-label">现场照片</div>
        <div v-if="imageList.length" class="image-list">
          <el-image
            v-for="(url, index) in imageList"
            :key="index"
            :src="url"
            :preview-src-list="imageList"
            :initial-index="index"
            fit="cover"
            class="order-image"
            preview-teleported
          >
            <template #error>
              <div class="image-error">图片加载失败</div>
            </template>
          </el-image>
        </div>
        <div v-else class="desc-text sub-text">未上传现场照片</div>
      </div>
    </div>

    <!-- ============ 处理进程（派单 / 维修 / 验收信息，由下游模块逐步回填） ============ -->
    <div class="page-card block-gap">
      <div class="card-title">处理进程</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="维修人员">{{ detail.workerName || '尚未派单' }}</el-descriptions-item>
        <el-descriptions-item label="派单管理员">{{ detail.dispatcherName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="派单时间">{{ detail.dispatchTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="派单备注">{{ detail.dispatchRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完工时间">{{ detail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修费用">
          {{ detail.totalCost === null || detail.totalCost === undefined ? '-' : `¥ ${detail.totalCost}` }}
        </el-descriptions-item>
        <el-descriptions-item label="验收时间">{{ detail.verifyTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="验收结果">
          <span v-if="detail.verifyResult === null || detail.verifyResult === undefined" class="sub-text">未验收</span>
          <el-tag v-else :type="detail.verifyResult === 1 ? 'success' : 'danger'" size="small">
            {{ detail.verifyResult === 1 ? '通过' : '驳回（返工）' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="验收意见">{{ detail.verifyRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否已评价">
          {{ detail.evaluated ? '已评价' : '未评价' }}
        </el-descriptions-item>
        <el-descriptions-item label="撤销原因">{{ detail.cancelReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="form-tip" style="margin-top: 8px">
        派单、维修与验收信息由后续模块（派单调度 / 维修进度 / 工单验收）逐步回填，未流转到的环节显示为空。
      </div>
    </div>

    <!-- ============ 撤销工单弹窗（O-05） ============ -->
    <el-dialog v-model="cancelVisible" title="撤销工单" width="480px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        撤销后工单状态将变为「已撤销」，不可再流转。
      </el-alert>
      <el-form ref="cancelRef" :model="cancelForm" :rules="cancelRules" label-width="90px">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, CircleClose, Edit, RefreshRight } from '@element-plus/icons-vue'
import { cancelOrder, getOrderDetail } from '@/api/order'
import { STATUS_FLOW, STATUS_HINT, statusName, statusTag, urgencyName, ORDER_STATUS } from '@/constants/order'

/**
 * 工单详情页（模块二 O-03）
 *
 * 三个展示要点：
 *  1. 「工单流转状态」时间轴基于 10 态字典，把工单在完整业务流程中的位置直观呈现出来，
 *     状态数据实时来自后端，下游模块更新后刷新即可见；
 *  2. 状态为 9「已驳回」时单独展示驳回原因 / 驳回人 / 驳回时间（FR-2-05），
 *     其余状态这些字段由后端保证为 null；
 *  3. 可执行操作完全依据服务端返回的 detail.canOperate，前端不重复实现角色与状态判断，
 *     避免前后端口径不一致。
 */
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref({})
const canOperate = computed(() => detail.value.canOperate || {})

/** 现场照片：字符串按英文逗号拆成数组，供 el-image 预览 */
const imageList = computed(() => {
  const raw = detail.value.images
  return raw ? raw.split(',').filter((url) => !!url) : []
})

// ======================= 状态时间轴 =======================
const flowSteps = computed(() =>
  STATUS_FLOW.map((code) => ({ code, name: ORDER_STATUS[code] || String(code) }))
)

/**
 * 主链上的当前进度下标。
 * 已驳回(9) 与 已撤销(7) 属于旁支终态，不在正常主链上，统一把进度停在起点（待审核），
 * 由顶部 alert 单独说明，避免时间轴出现「跳步」的误读。
 */
const flowActive = computed(() => {
  const status = detail.value.status
  const index = STATUS_FLOW.indexOf(status)
  return index < 0 ? 0 : index
})

/** 已驳回时把步骤条标为 error，其余保持正常推进色 */
const flowProcessStatus = computed(() => (detail.value.status === 9 ? 'error' : 'process'))

// ======================= 数据加载 =======================
async function fetchDetail () {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.orderId)
    detail.value = res.data || {}
  } catch (e) {
    // 403 无权限 / 404 不存在时由拦截器提示，这里退回列表页
    router.replace('/my-orders')
  } finally {
    loading.value = false
  }
}

// ======================= 页面跳转 =======================
/** @param {'edit'|'resubmit'} mode */
function goModify (mode) {
  router.push({ path: '/orders/submit', query: { orderId: detail.value.orderId, mode } })
}

function goBack () {
  router.back()
}

// ======================= 撤销工单（O-05） =======================
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelRef = ref(null)
const cancelForm = reactive({ cancelReason: '' })

const cancelRules = {
  cancelReason: [
    { required: true, message: '请填写撤销原因', trigger: 'blur' },
    { min: 5, max: 200, message: '撤销原因长度须为 5~200 个字符', trigger: 'blur' }
  ]
}

async function handleCancelSubmit () {
  if (!cancelRef.value) return
  const valid = await cancelRef.value.validate().catch(() => false)
  if (!valid) return

  cancelLoading.value = true
  try {
    const res = await cancelOrder(detail.value.orderId, cancelForm.cancelReason.trim())
    ElMessage.success(res.message || '撤销成功')
    cancelVisible.value = false
    cancelForm.cancelReason = ''
    await fetchDetail()
  } catch (e) {
    // 错误提示由响应拦截器统一处理
  } finally {
    cancelLoading.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-no {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.block-gap {
  margin-top: 12px;
}

.card-title {
  margin-bottom: 14px;
  padding-bottom: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}

.alert-line {
  font-size: 13px;
  line-height: 22px;
}

.flow-hint {
  margin-top: 14px;
  font-size: 13px;
  color: #606266;
  text-align: center;
}

.flow-extra {
  color: #909399;
}

.desc-block {
  margin-bottom: 16px;
}

.desc-label {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.desc-text {
  font-size: 14px;
  line-height: 22px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
}

.sub-text {
  font-size: 13px;
  color: #909399;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-image {
  width: 120px;
  height: 120px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #909399;
  background-color: #f5f7fa;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}
</style>
