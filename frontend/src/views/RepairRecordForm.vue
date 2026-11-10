<template>
  <div class="page-container" v-loading="loading">
    <div class="page-card">
      <div class="page-head">
        <div>
          <span class="page-title">维修记录填写</span>
          <el-tag v-if="order.status !== undefined" :type="statusTag(order.status)" size="small" effect="dark">
            {{ order.statusName || statusName(order.status) }}
          </el-tag>
        </div>
        <el-button :icon="Back" @click="goBack">返回我的工单</el-button>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页承担两件事：<b>① 追加维修记录</b>（R-02，可多次提交，不改变工单状态）；
        <b>② 提交维修完成</b>（R-04，状态 3 → 4 待验收）。
        维修记录一经写入即不可修改、不可删除，请如实填写。
      </el-alert>

      <!-- ============ 报修工单信息（O-03） ============ -->
      <el-descriptions title="报修工单信息" :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="工单编号">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修类目">{{ order.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修标题" :span="2">{{ order.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修地址" :span="2">{{ order.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">
          {{ order.contactName || '-' }}（{{ order.contactPhone || '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="期望上门">{{ order.expectTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag v-if="order.urgency === 1" type="danger" size="small" effect="dark">紧急</el-tag>
          <span v-else>普通</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计维修费用">
          <span class="cost">¥ {{ Number(order.totalCost || 0).toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">
          <div class="desc-text">{{ order.description || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab">
        <!-- ==================== 标签一：追加维修记录（R-02） ==================== -->
        <el-tab-pane label="追加维修记录" name="append">
          <el-alert
            v-if="!canAppend(order.status)"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          >
            当前状态为「{{ order.statusName || statusName(order.status) }}」，
            仅「已接单(2)」「维修中(3)」的工单可追加维修记录。
          </el-alert>

          <el-form ref="appendRef" :model="appendForm" :rules="appendRules" label-width="110px">
            <el-form-item label="记录类型" prop="recordType">
              <el-select v-model="appendForm.recordType" style="width: 100%">
                <el-option
                  v-for="item in APPENDABLE_RECORD_TYPES"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span class="option-hint">{{ item.hint }}</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="进度说明" prop="progressContent">
              <el-input
                v-model="appendForm.progressContent"
                type="textarea"
                :rows="4"
                :maxlength="PROGRESS_CONTENT_MAX"
                show-word-limit
                placeholder="请填写本次进度说明（2~200 字）。如需表示「暂停 / 等待配件」，选择「维修进度」并在文本中说明。"
              />
              <div class="phrase-row">
                <span class="phrase-label">快捷短语：</span>
                <el-tag
                  v-for="phrase in PROGRESS_QUICK_PHRASES"
                  :key="phrase"
                  size="small"
                  effect="plain"
                  class="phrase-tag"
                  @click="appendForm.progressContent = phrase"
                >
                  {{ phrase }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item label="维修照片">
              <div class="upload-wrap">
                <el-upload
                  v-model:file-list="appendFileList"
                  list-type="picture-card"
                  :limit="RECORD_IMAGES_MAX"
                  accept=".jpg,.jpeg,.png"
                  :http-request="handleUpload"
                  :before-upload="beforeUpload"
                  :on-exceed="handleExceed"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
                <div class="upload-tip">
                  仅支持 jpg / jpeg / png，单张 ≤ 5MB，最多 {{ RECORD_IMAGES_MAX }} 张
                </div>
              </div>
            </el-form-item>

            <el-form-item label="使用/更换配件">
              <el-input
                v-model="appendForm.partsUsed"
                :maxlength="PARTS_USED_MAX"
                show-word-limit
                placeholder="可选，如「不锈钢编织软管 1 根」"
              />
            </el-form-item>

            <el-form-item label="本次费用" prop="cost">
              <el-input-number
                v-model="appendForm.cost"
                :min="0"
                :max="99999999.99"
                :precision="2"
                :step="10"
                controls-position="right"
                style="width: 220px"
              />
              <span class="cost-unit">元</span>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!canAppend(order.status)"
                @click="handleAppendSubmit"
              >保存维修记录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 标签二：提交维修完成（R-04） ==================== -->
        <el-tab-pane label="提交维修完成" name="finish">
          <el-alert
            v-if="!canFinish(order.status)"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          >
            当前状态为「{{ order.statusName || statusName(order.status) }}」，
            仅「维修中(3)」的工单可提交维修完成。若尚未开始维修，请先在「我的工单」中点击「开始维修」。
          </el-alert>
          <el-alert v-else type="success" :closable="false" show-icon style="margin-bottom: 12px">
            提交后工单状态变为「<b>待验收(4)</b>」并记录完工时间，居民端立即可见并可发起验收。
            若居民验收驳回，工单会回到「维修中」，届时可再次提交。
          </el-alert>

          <el-form ref="finishRef" :model="finishForm" :rules="finishRules" label-width="110px">
            <el-form-item label="维修结果说明" prop="progressContent">
              <el-input
                v-model="finishForm.progressContent"
                type="textarea"
                :rows="5"
                :maxlength="FINISH_CONTENT_MAX"
                show-word-limit
                placeholder="必填，10~300 字。请说明故障原因、处理方式与维修结果，例：水槽下方软管老化破裂导致渗水，已更换不锈钢编织软管并做打压测试，无渗漏。"
              />
            </el-form-item>

            <el-form-item label="维修后照片">
              <div class="upload-wrap">
                <el-upload
                  v-model:file-list="finishFileList"
                  list-type="picture-card"
                  :limit="RECORD_IMAGES_MAX"
                  accept=".jpg,.jpeg,.png"
                  :http-request="handleUpload"
                  :before-upload="beforeUpload"
                  :on-exceed="handleExceed"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
                <div class="upload-tip">
                  仅支持 jpg / jpeg / png，单张 ≤ 5MB，最多 {{ RECORD_IMAGES_MAX }} 张
                </div>
              </div>
            </el-form-item>

            <el-form-item label="配件汇总">
              <el-input
                v-model="finishForm.partsUsed"
                :maxlength="PARTS_USED_MAX"
                show-word-limit
                placeholder="可选，本次维修合计使用的配件，如「不锈钢编织软管 1 根、生料带 1 卷」"
              />
            </el-form-item>

            <el-form-item label="完工结算费用" prop="cost">
              <el-input-number
                v-model="finishForm.cost"
                :min="0"
                :max="99999999.99"
                :precision="2"
                :step="10"
                controls-position="right"
                style="width: 220px"
              />
              <span class="cost-unit">元（将累加至工单费用合计）</span>
            </el-form-item>

            <el-form-item>
              <el-button
                type="success"
                :loading="submitting"
                :disabled="!canFinish(order.status)"
                @click="handleFinishSubmit"
              >提交维修完成</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============ 已有维修记录时间轴（R-01，就地展示便于对照填写） ============ -->
    <div class="page-card" v-loading="recordsLoading">
      <div class="card-title">已有维修记录（共 {{ records.length }} 条）</div>

      <el-empty v-if="!recordsLoading && records.length === 0" description="暂无维修记录" />

      <el-timeline v-else style="padding-left: 4px">
        <el-timeline-item
          v-for="item in records"
          :key="item.recordId"
          :timestamp="item.recordTime"
          placement="top"
          :type="recordTypeTimeline(item.recordType)"
        >
          <div class="record-head">
            <el-tag :type="recordTypeTag(item.recordType)" size="small" effect="dark">
              {{ item.recordTypeName || recordTypeName(item.recordType) }}
            </el-tag>
            <span class="sub-text">{{ item.workerName || '-' }}</span>
          </div>
          <div class="record-content">{{ item.progressContent }}</div>
          <div v-if="item.partsUsed" class="record-extra">配件：{{ item.partsUsed }}</div>
          <div v-if="item.cost !== null && item.cost !== undefined" class="record-extra">
            本次费用：<span class="cost">¥ {{ Number(item.cost).toFixed(2) }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api/order'
import { appendOrderRecord, finishRepair, listOrderRecords } from '@/api/progress'
import { uploadImage } from '@/api/file'
import { statusName, statusTag } from '@/constants/order'
import {
  APPENDABLE_RECORD_TYPES,
  FINISH_CONTENT_MAX,
  FINISH_CONTENT_MIN,
  PARTS_USED_MAX,
  PROGRESS_CONTENT_MAX,
  PROGRESS_CONTENT_MIN,
  PROGRESS_QUICK_PHRASES,
  RECORD_IMAGES_MAX,
  canAppend,
  canFinish,
  recordTypeName,
  recordTypeTag,
  recordTypeTimeline
} from '@/constants/progress'

/**
 * 维修记录填写页（模块四 · R-02 追加维修进度 + R-04 提交维修完成）
 *
 * 入口：我的工单列表 → 状态 3（维修中）行点击「提交完工」，或直接从列表进入本页补充记录。
 * 路由：/my-tasks/:orderId/record（meta.hidden，仅从列表页进入）。
 *
 * 页面分两块：
 *  ① 上半部分为报修工单信息（复用模块二 O-03 工单详情，服务端已做数据归属校验）；
 *  ② 下半部分为可编辑区：两个标签页分别对应 R-02 与 R-04；
 *  ③ 底部就地展示已有维修记录时间轴（R-01），便于填写时对照，避免重复上传。
 *
 * 全部业务规则判定以服务端为准：本页仅做「不可能状态」的按钮禁用与长度校验，
 * 不作为安全边界。
 */
const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.orderId)

const loading = ref(false)
const submitting = ref(false)
const recordsLoading = ref(false)
const activeTab = ref('append')

const order = ref({})
const records = ref([])

// ======================= 追加维修记录（R-02） =======================
const appendRef = ref(null)
const appendFileList = ref([])
const appendForm = reactive({
  recordType: 3,
  progressContent: '',
  partsUsed: '',
  cost: 0
})

const appendRules = {
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  progressContent: [
    { required: true, message: '请填写进度说明', trigger: 'blur' },
    {
      min: PROGRESS_CONTENT_MIN,
      max: PROGRESS_CONTENT_MAX,
      message: `进度说明长度须为 ${PROGRESS_CONTENT_MIN}~${PROGRESS_CONTENT_MAX} 个字符`,
      trigger: 'blur'
    }
  ],
  cost: [{ required: true, message: '请填写本次费用（无费用填 0）', trigger: 'blur' }]
}

// ======================= 提交维修完成（R-04） =======================
const finishRef = ref(null)
const finishFileList = ref([])
const finishForm = reactive({
  progressContent: '',
  partsUsed: '',
  cost: 0
})

const finishRules = {
  progressContent: [
    { required: true, message: '请填写维修结果说明', trigger: 'blur' },
    {
      min: FINISH_CONTENT_MIN,
      max: FINISH_CONTENT_MAX,
      message: `维修结果说明长度须为 ${FINISH_CONTENT_MIN}~${FINISH_CONTENT_MAX} 个字符`,
      trigger: 'blur'
    }
  ],
  cost: [{ required: true, message: '请填写完工结算费用（无费用填 0）', trigger: 'blur' }]
}

// ======================= 图片上传（复用模块零 U-15 公共上传接口） =======================

function beforeUpload (file) {
  const allowed = ['image/jpeg', 'image/jpg', 'image/png']
  if (!allowed.includes(file.type)) {
    ElMessage.warning('仅支持 jpg / jpeg / png 格式的图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('单张图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 覆盖 el-upload 默认上传行为，走统一封装的 U-15 接口（自动携带 JWT） */
async function handleUpload (options) {
  try {
    const res = await uploadImage(options.file)
    options.file.url = res.data.url
    options.onSuccess(res.data)
  } catch (e) {
    options.onError(e)
  }
}

function handleExceed () {
  ElMessage.warning(`维修照片最多上传 ${RECORD_IMAGES_MAX} 张`)
}

/** 把上传成功的路径按英文逗号拼接（对应 repair_record.images 字段口径） */
function buildImages (fileListRef) {
  return fileListRef.value
    .map((item) => item.url)
    .filter((url) => !!url)
    .join(',')
}

// ======================= 数据加载 =======================

async function loadOrder () {
  loading.value = true
  try {
    // O-03 工单详情（服务端校验：维修人员仅可查看本人受派工单）
    const res = await getOrderDetail(orderId)
    order.value = res.data || {}
  } catch (e) {
    // 无权限 / 工单不存在时返回列表页
    router.replace('/my-tasks')
  } finally {
    loading.value = false
  }
}

/** R-01：加载已有维修记录，供填写时对照 */
async function loadRecords () {
  recordsLoading.value = true
  try {
    const res = await listOrderRecords(orderId)
    records.value = res.data || []
  } catch (e) {
    records.value = []
  } finally {
    recordsLoading.value = false
  }
}

// ======================= 提交 =======================

async function handleAppendSubmit () {
  if (!appendRef.value) return
  const valid = await appendRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await appendOrderRecord(orderId, {
      recordType: appendForm.recordType,
      progressContent: appendForm.progressContent.trim(),
      images: buildImages(appendFileList) || undefined,
      partsUsed: appendForm.partsUsed.trim() || undefined,
      cost: appendForm.cost === null || appendForm.cost === undefined ? 0 : appendForm.cost
    })
    ElMessage.success(res.message || '维修记录已保存')
    // 重置填写区并刷新时间轴与费用合计
    appendRef.value.resetFields()
    appendFileList.value = []
    appendForm.recordType = 3
    appendForm.cost = 0
    await Promise.all([loadOrder(), loadRecords()])
  } catch (e) {
    // 400 / 403 / 409 提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function handleFinishSubmit () {
  if (!finishRef.value) return
  const valid = await finishRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm(
      '提交后工单将流转为「待验收(4)」，居民端立即可见并发起验收。确认提交吗？',
      '提交维修完成',
      { confirmButtonText: '确认提交', cancelButtonText: '再检查一下', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }

  submitting.value = true
  try {
    const res = await finishRepair(orderId, {
      progressContent: finishForm.progressContent.trim(),
      images: buildImages(finishFileList) || undefined,
      partsUsed: finishForm.partsUsed.trim() || undefined,
      cost: finishForm.cost === null || finishForm.cost === undefined ? 0 : finishForm.cost
    })
    ElMessage.success(res.message || '已提交维修完成，等待居民验收')
    await Promise.all([loadOrder(), loadRecords()])
    activeTab.value = 'append'
  } catch (e) {
    // 400 / 403 / 409 提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function goBack () {
  router.push('/my-tasks')
}

onMounted(async () => {
  await loadOrder()
  await loadRecords()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-right: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.desc-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.option-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.phrase-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

.phrase-label {
  font-size: 12px;
  color: #909399;
}

.phrase-tag {
  cursor: pointer;
}

.upload-wrap {
  width: 100%;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.cost-unit {
  margin-left: 8px;
  color: #606266;
}

.cost {
  color: #e6a23c;
  font-weight: 600;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}

.record-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.record-content {
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
  line-height: 1.6;
}

.record-extra {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
}

.page-card + .page-card {
  margin-top: 16px;
}
</style>
