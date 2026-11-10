<template>
  <el-dialog
    v-model="visible"
    title="更新维修进度"
    width="640px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
      对应接口 <b>R-02 追加维修进度</b>：追加记录<b>不改变工单状态</b>，
      可多次提交，每次独立留痕（记录不可修改、不可删除）。
      本次费用会自动累加至工单的维修费用合计。
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="工单编号">
        <span>{{ orderNo || '-' }}</span>
      </el-form-item>

      <el-form-item label="记录类型" prop="recordType">
        <el-select v-model="form.recordType" placeholder="请选择记录类型" style="width: 100%">
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
          v-model="form.progressContent"
          type="textarea"
          :rows="4"
          :maxlength="PROGRESS_CONTENT_MAX"
          show-word-limit
          placeholder="请填写本次进度说明（2~200 字）。如需表示「暂停 / 等待配件」，选择记录类型「维修进度」并在文本中说明即可。"
        />
        <div class="phrase-row">
          <span class="phrase-label">快捷短语：</span>
          <el-tag
            v-for="phrase in PROGRESS_QUICK_PHRASES"
            :key="phrase"
            size="small"
            effect="plain"
            class="phrase-tag"
            @click="applyPhrase(phrase)"
          >
            {{ phrase }}
          </el-tag>
        </div>
      </el-form-item>

      <el-form-item label="维修照片">
        <div class="upload-wrap">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :limit="RECORD_IMAGES_MAX"
            accept=".jpg,.jpeg,.png"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            :on-exceed="handleExceed"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">仅支持 jpg / jpeg / png，单张 ≤ 5MB，最多 {{ RECORD_IMAGES_MAX }} 张</div>
        </div>
      </el-form-item>

      <el-form-item label="使用/更换配件">
        <el-input
          v-model="form.partsUsed"
          :maxlength="PARTS_USED_MAX"
          show-word-limit
          placeholder="可选，如「不锈钢编织软管 1 根」"
        />
      </el-form-item>

      <el-form-item label="本次费用" prop="cost">
        <el-input-number
          v-model="form.cost"
          :min="0"
          :max="99999999.99"
          :precision="2"
          :step="10"
          controls-position="right"
          style="width: 220px"
        />
        <span class="cost-unit">元</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交进度</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { appendOrderRecord } from '@/api/progress'
import { uploadImage } from '@/api/file'
import {
  APPENDABLE_RECORD_TYPES,
  PARTS_USED_MAX,
  PROGRESS_CONTENT_MAX,
  PROGRESS_CONTENT_MIN,
  PROGRESS_QUICK_PHRASES,
  RECORD_IMAGES_MAX
} from '@/constants/progress'

/**
 * 进度更新弹窗（模块四 R-02 追加维修进度）
 *
 * 由「我的工单列表」与「维修记录填写页」共用：维修人员选择记录类型
 * （维修进度 / 到达现场 / 更换配件），填写进度说明、上传维修过程照片、
 * 登记配件与本次费用，提交后追加一条独立的维修记录。
 *
 * 服务端校验（前端仅做界面引导，不作安全边界）：
 *  ① 工单必须指派给当前维修人员，否则 403；
 *  ② 工单状态须为 2（已接单）或 3（维修中），否则 409；
 *  ③ 进度说明 2~200 字、照片 ≤ 3 张、配件 ≤ 255 字、费用非负，否则 400。
 */
const props = defineProps({
  /** 弹窗显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 工单 ID */
  orderId: { type: [Number, String], default: null },
  /** 工单编号，仅用于展示 */
  orderNo: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, (val) => { visible.value = val })
watch(visible, (val) => emit('update:modelValue', val))

const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])

const form = reactive({
  recordType: 3,
  progressContent: '',
  partsUsed: '',
  cost: 0
})

const rules = {
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

/** 点击快捷短语直接填入进度说明，降低手机端输入成本 */
function applyPhrase (phrase) {
  form.progressContent = phrase
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
    // 把服务端返回的相对路径写回上传项，供缩略图预览与最终拼接 images 使用
    options.file.url = res.data.url
    options.onSuccess(res.data)
  } catch (e) {
    // 错误提示由响应拦截器统一处理
    options.onError(e)
  }
}

function handleExceed () {
  ElMessage.warning(`维修过程照片最多上传 ${RECORD_IMAGES_MAX} 张`)
}

/** 提交时把已上传成功的图片路径按英文逗号拼接（对应 repair_record.images 字段口径） */
function buildImages () {
  return fileList.value
    .map((item) => item.url)
    .filter((url) => !!url)
    .join(',')
}

// ======================= 提交 =======================

async function handleSubmit () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await appendOrderRecord(props.orderId, {
      recordType: form.recordType,
      progressContent: form.progressContent.trim(),
      images: buildImages() || undefined,
      partsUsed: form.partsUsed.trim() || undefined,
      cost: form.cost === null || form.cost === undefined ? 0 : form.cost
    })
    ElMessage.success(res.message || '进度已更新')
    visible.value = false
    emit('success')
  } catch (e) {
    // 400 / 403 / 409 提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

/** 关闭后重置表单，避免下次打开残留上一次的内容 */
function handleClosed () {
  formRef.value?.resetFields()
  fileList.value = []
  form.recordType = 3
  form.progressContent = ''
  form.partsUsed = ''
  form.cost = 0
}
</script>

<style scoped>
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
</style>
