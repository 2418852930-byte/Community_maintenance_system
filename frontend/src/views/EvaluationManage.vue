<template>
  <div class="page-container">
    <div class="page-card">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本页为<b>评价管理</b>：展示<b>全部</b>服务评价（管理员不受数据范围限制），
        支持按<b>综合评分、评分上限、报修类目、维修人员、是否已回复、评价时间</b>筛选。
        依据 BR-6-07，一条评价<b>只能回复一次，回复后不可修改</b>；
        评价数据同时开放给下游统计分析模块调用（FR-6-08 由统计模块 S-07 聚合）。
      </el-alert>

      <!-- ============ 搜索区（E-02 查询条件，管理员可用全部筛选维度） ============ -->
      <el-form :model="query" inline>
        <el-form-item label="综合评分">
          <el-select v-model="query.totalScore" placeholder="全部评分" clearable style="width: 160px">
            <el-option
              v-for="item in SCORE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="评分上限">
          <el-select v-model="query.maxScore" placeholder="不限" clearable style="width: 150px">
            <el-option label="≤ 2 星（低分工单）" :value="2" />
            <el-option label="≤ 3 星" :value="3" />
            <el-option label="≤ 4 星" :value="4" />
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

        <el-form-item label="维修人员">
          <el-select v-model="query.workerId" placeholder="全部人员" clearable filterable style="width: 160px">
            <el-option
              v-for="item in workerOptions"
              :key="item.workerId"
              :label="item.realName"
              :value="item.workerId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="回复状态">
          <el-select v-model="query.hasReply" placeholder="全部" clearable style="width: 140px">
            <el-option label="已回复" :value="true" />
            <el-option label="未回复" :value="false" />
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

      <!-- ============ 工具条 ============ -->
      <div class="toolbar">
        <el-tag type="success" effect="dark" size="large">评价总数 {{ total }} 条</el-tag>
        <el-tag type="warning" effect="plain">未回复 {{ unrepliedCount }} 条</el-tag>
        <span class="toolbar-tip">低分工单可通过「评分上限」筛选后重点跟进</span>
      </div>

      <!-- ============ 评价列表（E-02） ============ -->
      <el-table v-loading="loading" :data="tableData" style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" width="150" align="center" />
        <el-table-column prop="categoryName" label="报修类目" min-width="140" show-overflow-tooltip />

        <el-table-column label="评价人" width="110" align="center">
          <template #default="{ row }">
            {{ row.residentName || '-' }}
            <el-tag v-if="row.isAnonymous === 1" type="info" size="small" effect="plain">匿名</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="维修人员" width="100" align="center">
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

        <el-table-column label="评价内容" min-width="180">
          <template #default="{ row }">
            <span v-if="row.content" class="content-text">{{ row.content }}</span>
            <span v-else class="empty-text">（未填写文字评价）</span>
          </template>
        </el-table-column>

        <el-table-column label="管理员回复" min-width="180">
          <template #default="{ row }">
            <span v-if="row.replyContent" class="content-text">{{ row.replyContent }}</span>
            <el-tag v-else type="warning" size="small" effect="plain">未回复</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="评价时间" width="160" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>

        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="!row.replyContent"
              link
              type="success"
              size="small"
              @click="openReply(row)"
            >回复</el-button>
            <el-tooltip v-else content="该评价已回复，不可重复回复（BR-6-07）" placement="top">
              <span class="disabled-text">已回复</span>
            </el-tooltip>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无评价数据：工单验收通过后居民即可提交评价" />
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

    <!-- ============ 回复评价弹窗（E-04） ============ -->
    <el-dialog
      v-model="replyVisible"
      title="回复评价"
      width="560px"
      :close-on-click-modal="false"
      @closed="handleReplyClosed"
    >
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
        依据 BR-6-07，一条评价<b>只能回复一次，回复后不可修改</b>，请确认内容后再提交。
      </el-alert>

      <el-descriptions :column="1" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="工单编号">{{ currentRow.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价人">
          {{ currentRow.residentName || '-' }}
          <el-tag v-if="currentRow.isAnonymous === 1" type="info" size="small" effect="plain">匿名</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="维修人员">{{ currentRow.workerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="综合评分">
          <el-tag :type="scoreTag(currentRow.totalScore)" size="small" effect="dark">
            {{ currentRow.totalScore }} 星
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评价内容">
          <div class="content-text">{{ currentRow.content || '（未填写文字评价）' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="90px">
        <el-form-item label="回复内容" prop="replyContent">
          <el-input
            v-model="replyForm.replyContent"
            type="textarea"
            :rows="4"
            :maxlength="REPLY_MAX"
            show-word-limit
            placeholder="请填写回复内容（5~200 字）"
          />
        </el-form-item>

        <el-form-item label="快捷短语">
          <div class="quick-phrases">
            <el-tag
              v-for="(phrase, idx) in REPLY_QUICK_PHRASES"
              :key="idx"
              class="quick-phrase"
              type="info"
              effect="plain"
              @click="replyForm.replyContent = phrase"
            >
              {{ phrase }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="handleReplySubmit">
          提交回复
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 评价详情弹窗（E-03） ============ -->
    <EvaluationDetailDialog
      v-model="detailVisible"
      :evaluation-id="currentEvaluationId"
      :order-no="currentEvaluationOrderNo"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageEvaluations, replyEvaluation } from '@/api/evaluation'
import { getCategoryTree } from '@/api/category'
import { getWorkerWorkload } from '@/api/dispatch'
import {
  REPLY_MAX,
  REPLY_MIN,
  REPLY_QUICK_PHRASES,
  SCORE_OPTIONS,
  scoreTag,
  scoreText
} from '@/constants/evaluation'
import EvaluationDetailDialog from '@/components/EvaluationDetailDialog.vue'

/**
 * 评价管理（模块六：管理员评价管理页，FR-6-05 / FR-6-06）
 *
 * 数据来源：E-02 评价分页列表（管理员不限定数据范围，可查看全部评价）。
 * 管理员专属筛选维度：
 *  - 维修人员（workerId）—— 接口文档 4.7.2 标注「仅管理员可用」；
 *  - 报修类目（categoryId）—— 同上。
 * 另外支持综合评分精确筛选、评分上限筛选（查低分工单）、是否已回复筛选与评价时间区间。
 *
 * 回复能力对应 E-04，服务端以「reply_content IS NULL」的条件更新保证
 * 「一条评价只能回复一次、回复后不可修改」（BR-6-07）。
 */
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  totalScore: null,
  maxScore: null,
  categoryId: null,
  workerId: null,
  hasReply: null
})

// ======================= 筛选下拉数据源（复用模块一 C-02 与模块三 D-01） =======================
const categoryOptions = ref([])
const workerOptions = ref([])

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

async function fetchWorkerOptions () {
  try {
    const res = await getWorkerWorkload({ onlyEnabled: true })
    workerOptions.value = res.data || []
  } catch (e) {
    workerOptions.value = []
  }
}

// ======================= 列表查询（E-02） =======================
/** 未回复条数：仅用于工具条提示，取自当前页数据（完整统计由统计模块 S-07 承担） */
const unrepliedCount = ref(0)

async function fetchList () {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      totalScore: query.totalScore === null || query.totalScore === '' ? undefined : query.totalScore,
      maxScore: query.maxScore === null || query.maxScore === '' ? undefined : query.maxScore,
      categoryId: query.categoryId === null || query.categoryId === '' ? undefined : query.categoryId,
      workerId: query.workerId === null || query.workerId === '' ? undefined : query.workerId,
      hasReply: query.hasReply === null ? undefined : query.hasReply,
      startTime: timeRange.value && timeRange.value[0] ? timeRange.value[0] : undefined,
      endTime: timeRange.value && timeRange.value[1] ? timeRange.value[1] : undefined
    }
    const res = await pageEvaluations(params)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
    unrepliedCount.value = tableData.value.filter((item) => !item.replyContent).length
  } catch (e) {
    tableData.value = []
    total.value = 0
    unrepliedCount.value = 0
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
  query.maxScore = null
  query.categoryId = null
  query.workerId = null
  query.hasReply = null
  timeRange.value = []
  query.pageNum = 1
  fetchList()
}

// ======================= 回复评价（E-04） =======================
const replyVisible = ref(false)
const replyLoading = ref(false)
const replyFormRef = ref(null)
const currentRow = reactive({})
const replyForm = reactive({ replyContent: '' })

const replyRules = {
  replyContent: [
    { required: true, message: '请填写回复内容', trigger: 'blur' },
    { min: REPLY_MIN, max: REPLY_MAX, message: `回复内容须为 ${REPLY_MIN}~${REPLY_MAX} 个字符`, trigger: 'blur' }
  ]
}

function openReply (row) {
  Object.assign(currentRow, row)
  replyForm.replyContent = ''
  replyVisible.value = true
}

function handleReplyClosed () {
  replyForm.replyContent = ''
  replyFormRef.value?.clearValidate()
}

async function handleReplySubmit () {
  if (!currentRow.evaluationId) return
  const valid = await replyFormRef.value.validate().catch(() => false)
  if (!valid) return

  replyLoading.value = true
  try {
    const res = await replyEvaluation(currentRow.evaluationId, {
      replyContent: replyForm.replyContent.trim()
    })
    ElMessage.success(res.message || '回复成功')
    replyVisible.value = false
    fetchList()
  } catch (e) {
    // 409（该评价已回复）由响应拦截器统一提示
  } finally {
    replyLoading.value = false
  }
}

// ======================= 评价详情（E-03） =======================
const currentEvaluationId = ref(null)
const currentEvaluationOrderNo = ref('')
const detailVisible = ref(false)

function openDetail (row) {
  currentEvaluationId.value = row.evaluationId
  currentEvaluationOrderNo.value = row.orderNo || ''
  detailVisible.value = true
}

onMounted(async () => {
  await Promise.all([fetchCategoryOptions(), fetchWorkerOptions()])
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

.disabled-text {
  font-size: 12px;
  color: #c0c4cc;
}

.quick-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-phrase {
  cursor: pointer;
}
</style>
