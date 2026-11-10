<template>
  <el-dialog
    :model-value="modelValue"
    title="服务评价"
    width="620px"
    :close-on-click-modal="false"
    @update:model-value="handleVisibleChange"
    @closed="handleClosed"
  >
    <el-alert type="success" :closable="false" show-icon style="margin-bottom: 12px">
      本次评价提交后，工单状态将由 <b>已验收(5)</b> 流转为 <b>已完成(6)</b>，
      工单业务链路闭环结束。依据 BR-6-02 <b>一个工单只能评价一次</b>，
      且评价提交后<b>不可修改或删除</b>（BR-6-05），请确认后再提交。
    </el-alert>

    <!-- 工单摘要：字段固定且数量少，用 el-descriptions 比表格更省空间 -->
    <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
      <el-descriptions-item label="工单编号">{{ order?.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修类目">{{ order?.categoryName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修标题" :span="2">{{ order?.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="维修人员">{{ order?.workerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="维修费用">
        <span class="cost">¥ {{ Number(order?.totalCost || 0).toFixed(2) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <!-- ============ 三项星级评分（BR-6-03：均为 1~5 的整数，必填） ============ -->
      <el-form-item
        v-for="dim in SCORE_DIMENSIONS"
        :key="dim.key"
        :label="dim.label"
        :prop="dim.key"
        required
      >
        <el-rate
          v-model="form[dim.key]"
          :max="SCORE_MAX"
          show-score
          :score-template="`{value} 星`"
          :texts="RATE_TEXTS"
        />
        <span class="dim-desc">{{ dim.desc }}</span>
      </el-form-item>

      <!-- ============ 综合评分预览（服务端按三项均值四舍五入计算，前端仅做预览） ============ -->
      <el-form-item label="综合评分">
        <el-tag :type="scoreTag(totalScorePreview)" size="large" effect="dark">
          {{ totalScorePreview }} 星 · {{ scoreText(totalScorePreview) }}
        </el-tag>
        <span class="dim-desc">综合评分 = 三项评分平均值四舍五入，由服务端计算</span>
      </el-form-item>

      <!-- ============ 评价内容（选填，0~200 字） ============ -->
      <el-form-item label="评价内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          :maxlength="CONTENT_MAX"
          show-word-limit
          placeholder="选填：说说本次维修服务的体验（0~200 字）"
        />
      </el-form-item>

      <!-- 快捷短语：降低手机端输入成本，与模块四、模块五的交互保持一致 -->
      <el-form-item label="快捷短语">
        <div class="quick-phrases">
          <el-tag
            v-for="(phrase, idx) in CONTENT_QUICK_PHRASES"
            :key="idx"
            class="quick-phrase"
            type="info"
            effect="plain"
            @click="form.content = phrase"
          >
            {{ phrase }}
          </el-tag>
        </div>
      </el-form-item>

      <!-- ============ 评价图片（选填，最多 3 张，复用模块零 U-15 上传接口） ============ -->
      <el-form-item label="评价图片">
        <el-upload
          v-model:file-list="fileList"
          list-type="picture-card"
          accept="image/jpeg,image/jpg,image/png"
          :limit="IMAGES_MAX"
          :http-request="doUpload"
          :on-exceed="handleExceed"
          :on-preview="handlePreview"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">选填，最多 {{ IMAGES_MAX }} 张，仅支持 jpg / png，单张不超过 5MB</div>
      </el-form-item>

      <!-- ============ 是否匿名（BR-6-06） ============ -->
      <el-form-item label="是否匿名">
        <el-switch
          v-model="form.isAnonymous"
          :active-value="1"
          :inactive-value="0"
          active-text="匿名评价"
          inactive-text="实名评价"
          inline-prompt
        />
        <span class="dim-desc">
          匿名后，维修人员与管理员在评价列表中<b>看不到您的姓名</b>（BR-6-06）；您本人仍可看到自己的评价
        </span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">提交评价</el-button>
    </template>

    <!-- 图片大图预览 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="600px" append-to-body>
      <img :src="previewUrl" alt="评价图片" style="width: 100%" />
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createEvaluation } from '@/api/evaluation'
import { uploadImage } from '@/api/file'
import {
  CONTENT_MAX,
  CONTENT_QUICK_PHRASES,
  IMAGES_MAX,
  SCORE_DIMENSIONS,
  SCORE_MAX,
  scoreTag,
  scoreText
} from '@/constants/evaluation'

/**
 * 评价提交弹窗（模块六 E-01「提交服务评价」）
 *
 * 交互要点：
 *  - 三项评分（服务态度 / 响应速度 / 维修质量）均必填，1~5 星（BR-6-03）；
 *  - 综合评价由前端实时预览、服务端最终计算，避免客户端伪造综合分；
 *  - 评价内容与图片均选填，图片复用模块零 U-15 上传接口（最多 3 张）；
 *  - 支持匿名开关（BR-6-06），匿名后对非本人隐藏评价人信息。
 *
 * 注意：前端校验只用于「提前给出友好提示」，真正的状态校验（仅状态 5 可评价、一单一评）
 * 全部由后端 Service 层执行（非报修人返回 403，状态非 5 或重复评价返回 409）。
 */
const props = defineProps({
  /** 弹窗显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 目标工单（待评价列表行对象，取 orderId / orderNo / title 等展示字段） */
  order: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

/** el-rate 的文字提示 */
const RATE_TEXTS = ['很差', '较差', '一般', '满意', '非常满意']

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  scoreService: 0,
  scoreSpeed: 0,
  scoreQuality: 0,
  content: '',
  isAnonymous: 0
})

/** 校验规则：三项评分必填（>0 即通过），内容长度由 maxlength 与后端双重兜底 */
const rules = computed(() => {
  const scoreRules = {}
  SCORE_DIMENSIONS.forEach((dim) => {
    scoreRules[dim.key] = [
      {
        validator: (rule, value, callback) => {
          if (!value || value < 1) {
            return callback(new Error(`请为「${dim.label}」打分`))
          }
          return callback()
        },
        trigger: 'change'
      }
    ]
  })
  return scoreRules
})

/** 综合评分预览：三项均值四舍五入，与后端 EvaluationConverter.computeTotalScore 同口径 */
const totalScorePreview = computed(() => {
  const values = SCORE_DIMENSIONS.map((dim) => Number(form[dim.key]) || 0)
  if (values.some((v) => v < 1)) return 0
  const sum = values.reduce((acc, cur) => acc + cur, 0)
  return Math.round(sum / values.length)
})

// ======================= 图片上传 =======================
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')

/**
 * uid -> 服务端返回的图片相对路径。
 * 单独用 Map 记录而不是改写 el-upload 内部行对象的 url，
 * 是因为 picture-card 模式下 el-upload 会自行写入 blob 预览地址，
 * 二者混用容易出现「提交上去的是 blob 地址」这类隐患。
 */
const uploadedUrls = ref({})

/**
 * 自定义上传：走模块零 U-15 接口（POST /api/files/upload）
 * 上传成功后把后端返回的相对路径记入 uploadedUrls，并标记该项上传成功
 */
async function doUpload (options) {
  try {
    const res = await uploadImage(options.file)
    uploadedUrls.value[options.file.uid] = res.data?.url || ''
    options.onSuccess(res.data)
  } catch (e) {
    options.onError(e)
    // 上传失败时从列表移除，避免提交出一堆空路径
    fileList.value = fileList.value.filter((item) => item.uid !== options.file.uid)
  }
}

function handleExceed () {
  ElMessage.warning(`评价图片最多上传 ${IMAGES_MAX} 张`)
}

function handlePreview (file) {
  previewUrl.value = uploadedUrls.value[file.uid] || file.url
  previewVisible.value = true
}

/** 收集已上传图片路径，拼成英文逗号分隔串（仅取上传成功的项，且过滤掉空路径） */
function collectImages () {
  return fileList.value
    .filter((item) => item.status === 'success')
    .map((item) => uploadedUrls.value[item.uid])
    .filter((url) => !!url)
    .join(',')
}

// ======================= 提交与重置 =======================
function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

/** 关闭后清空表单、图片与校验态，避免下次打开残留上一次的内容 */
function handleClosed () {
  form.scoreService = 0
  form.scoreSpeed = 0
  form.scoreQuality = 0
  form.content = ''
  form.isAnonymous = 0
  fileList.value = []
  uploadedUrls.value = {}
  formRef.value?.clearValidate()
}

async function handleSubmit () {
  if (!props.order?.orderId) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const images = collectImages()
    const res = await createEvaluation({
      orderId: props.order.orderId,
      scoreService: form.scoreService,
      scoreSpeed: form.scoreSpeed,
      scoreQuality: form.scoreQuality,
      // 内容留空则不传（落库为 null），避免存入空字符串
      content: form.content.trim() || undefined,
      images: images || undefined,
      isAnonymous: form.isAnonymous
    })
    ElMessage.success(res.message || '评价提交成功')
    emit('update:modelValue', false)
    emit('success', res.data)
  } catch (e) {
    // 403（非本人工单）/ 409（工单尚未验收、重复评价）由响应拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.cost {
  color: #e6a23c;
  font-weight: 600;
}

.dim-desc {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.quick-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-phrase {
  cursor: pointer;
}

.upload-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
