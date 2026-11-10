<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="640px"
    @update:model-value="handleVisibleChange"
    @open="fetchDetail"
  >
    <div v-loading="loading">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本弹窗数据来源为<b>服务评价表</b>（<code>repair_evaluation</code>），
        经模块六 E-03「评价详情」接口返回，并附带对应工单摘要。
        评价一经提交即不可修改或删除（BR-6-05）。
      </el-alert>

      <el-empty v-if="!loading && !detail" description="暂无评价信息" />

      <template v-else-if="detail">
        <!-- ============ 工单摘要（E-03 的 order 子对象） ============ -->
        <el-descriptions title="工单摘要" :column="2" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="工单编号">{{ detail.order?.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报修类目">{{ detail.order?.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报修标题" :span="2">
            {{ detail.order?.title || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ detail.order?.finishTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修费用">
            <span class="cost">¥ {{ Number(detail.order?.totalCost || 0).toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- ============ 评价主体（字段与 E-02 列表元素一致） ============ -->
        <el-descriptions title="评价信息" :column="2" border size="small">
          <el-descriptions-item label="评价时间">{{ detail.evaluation?.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评价人">
            {{ detail.evaluation?.residentName || '-' }}
            <el-tag :type="detail.evaluation?.isAnonymous === 1 ? 'info' : 'success'" size="small" effect="plain">
              {{ anonymousText(detail.evaluation?.isAnonymous) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="维修人员">{{ detail.evaluation?.workerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">
            <el-tag :type="scoreTag(detail.evaluation?.totalScore)" size="small" effect="dark">
              {{ detail.evaluation?.totalScore }} 星 · {{ scoreText(detail.evaluation?.totalScore) }}
            </el-tag>
          </el-descriptions-item>

          <!-- 三项维度评分 -->
          <el-descriptions-item
            v-for="dim in SCORE_DIMENSIONS"
            :key="dim.key"
            :label="dim.label"
            :span="1"
          >
            <el-rate :model-value="Number(detail.evaluation?.[dim.key]) || 0" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="是否匿名">
            {{ anonymousText(detail.evaluation?.isAnonymous) }}
          </el-descriptions-item>

          <el-descriptions-item label="评价内容" :span="2">
            <div class="content-text">{{ detail.evaluation?.content || '（未填写文字评价）' }}</div>
          </el-descriptions-item>

          <el-descriptions-item label="评价图片" :span="2">
            <div v-if="images.length" class="image-list">
              <el-image
                v-for="(url, idx) in images"
                :key="idx"
                :src="url"
                :preview-src-list="images"
                :initial-index="idx"
                fit="cover"
                style="width: 88px; height: 88px; border-radius: 4px"
              />
            </div>
            <span v-else class="empty-text">无图片</span>
          </el-descriptions-item>

          <el-descriptions-item label="管理员回复" :span="2">
            <div v-if="detail.evaluation?.replyContent">
              <div class="content-text">{{ detail.evaluation.replyContent }}</div>
              <div class="reply-time">回复时间：{{ detail.evaluation.replyTime || '-' }}</div>
            </div>
            <span v-else class="empty-text">暂未回复</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { getEvaluationDetail } from '@/api/evaluation'
import {
  anonymousText,
  parseImages,
  SCORE_DIMENSIONS,
  scoreTag,
  scoreText
} from '@/constants/evaluation'

/**
 * 评价详情弹窗（模块六 E-03「评价详情」）
 *
 * 公共展示组件：居民「我的评价」与管理员「评价管理」的「详情」按钮均进入本弹窗。
 * 服务端按角色做数据归属校验（居民仅本人、维修人员仅本人相关、管理员全部），
 * 越权返回 403，不存在返回 404。
 *
 * 匿名脱敏由服务端完成（BR-6-06）：非本人查看匿名评价时，
 * 服务端返回的 residentId 为 null、residentName 为「匿名用户」，
 * 前端只做展示，不承担任何脱敏职责（避免前端被绕过后泄露隐私）。
 */
const props = defineProps({
  /** 弹窗显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 评价 ID */
  evaluationId: { type: [Number, String], default: null },
  /** 工单编号，仅用于标题展示 */
  orderNo: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const detail = ref(null)

const dialogTitle = computed(() =>
  props.orderNo ? `评价详情 · ${props.orderNo}` : '评价详情'
)

/** 评价图片路径（逗号分隔）-> 数组，直接喂给 el-image 的预览列表 */
const images = computed(() => parseImages(detail.value?.evaluation?.images))

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

async function fetchDetail () {
  if (!props.evaluationId) return
  loading.value = true
  try {
    const res = await getEvaluationDetail(props.evaluationId)
    detail.value = res.data || null
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

defineExpose({ fetchDetail })
</script>

<style scoped>
.cost {
  color: #e6a23c;
  font-weight: 600;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.reply-time {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
