<template>
  <el-dialog
    :model-value="modelValue"
    :title="isAgent ? '管理员代验收' : '验收确认'"
    width="560px"
    @update:model-value="handleVisibleChange"
    @closed="handleClosed"
  >
    <el-alert
      :type="isAgent ? 'warning' : 'success'"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    >
      <template v-if="isAgent">
        本操作将代表居民完成验收，工单状态由 <b>待验收(4)</b> 流转为 <b>已验收(5)</b>，
        并在验收记录中标识为「<b>管理员代验收</b>」（BR-5-07）。
      </template>
      <template v-else>
        验收通过后工单状态由 <b>待验收(4)</b> 流转为 <b>已验收(5)</b>，
        并解锁该工单的服务评价入口。请确认维修结果已符合预期。
      </template>
    </el-alert>

    <!-- 工单摘要：数量少、字段固定，用 el-descriptions 比表格更省空间 -->
    <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
      <el-descriptions-item label="工单编号">{{ order?.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修类目">{{ order?.categoryName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修标题" :span="2">{{ order?.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="维修人员">{{ order?.workerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="维修费用">
        <span class="cost">¥ {{ Number(order?.totalCost || 0).toFixed(2) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item :label="isAgent ? '代验收说明' : '验收意见'" prop="verifyRemark">
        <el-input
          v-model="form.verifyRemark"
          type="textarea"
          :rows="4"
          :maxlength="VERIFY_REMARK_MAX"
          show-word-limit
          :placeholder="remarkPlaceholder"
        />
      </el-form-item>

      <!-- 快捷短语：降低手机端输入成本，与模块四进度说明的交互保持一致 -->
      <el-form-item label="快捷短语">
        <div class="quick-phrases">
          <el-tag
            v-for="(phrase, idx) in quickPhrases"
            :key="idx"
            class="quick-phrase"
            type="info"
            effect="plain"
            @click="applyPhrase(phrase)"
          >
            {{ phrase }}
          </el-tag>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">取消</el-button>
      <el-button type="success" :loading="loading" @click="handleSubmit">
        确认验收通过
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentVerifyOrder, verifyOrder } from '@/api/verify'
import {
  AGENT_REMARK_MAX,
  AGENT_REMARK_MIN,
  PASS_QUICK_PHRASES,
  VERIFY_REMARK_MAX,
  VERIFY_REMARK_MIN
} from '@/constants/verify'

/**
 * 验收确认弹窗（模块五 V-02「验收通过」/ V-03「管理员代验收」）
 *
 * **一个组件承载两种身份的同一种动作**——居民点「验收」与管理员的「代验收」
 * 在交互上完全一致（都是确认工单合格并填写说明），差异只在
 * 「说明是否必填」与「调用哪个接口」，因此用 mode 属性区分，
 * 避免为两者各写一份几乎相同的弹窗（与模块三、模块四的组件复用思路一致）。
 *
 *  - mode = 'verify'：居民验收，调用 V-02（verifyResult = 1），验收意见可选；
 *  - mode = 'agent' ：管理员代验收，调用 V-03，代验收说明必填 5~200 字（BR-5-07）。
 *
 * 注意：前端校验只用于「提前给出友好提示」，真正的状态流转与数据归属校验
 * 全部由后端 Service 层执行（非本人提交的工单会返回 403，非待验收状态返回 409）。
 */
const props = defineProps({
  /** 弹窗显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 目标工单（列表行对象，取 orderId / orderNo / title 等展示字段） */
  order: { type: Object, default: null },
  /** 操作模式：'verify' 居民验收 / 'agent' 管理员代验收 */
  mode: { type: String, default: 'verify' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const isAgent = computed(() => props.mode === 'agent')
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ verifyRemark: '' })

/** 代验收说明必填、验收意见可选（填写则受 5~200 字约束） */
const rules = computed(() => ({
  verifyRemark: isAgent.value
    ? [
        { required: true, message: '请填写代验收说明', trigger: 'blur' },
        { min: AGENT_REMARK_MIN, max: AGENT_REMARK_MAX, message: '代验收说明须为 5~200 个字符', trigger: 'blur' }
      ]
    : [
        {
          validator: (rule, value, callback) => {
            // 通过时意见可留空；一旦填写，则必须是完整的 5~200 字意见
            if (!value || !value.trim()) return callback()
            const len = value.trim().length
            if (len < VERIFY_REMARK_MIN || len > VERIFY_REMARK_MAX) {
              return callback(new Error('验收意见长度须为 5~200 个字符（留空表示无补充意见）'))
            }
            return callback()
          },
          trigger: 'blur'
        }
      ]
}))

const remarkPlaceholder = computed(() =>
  isAgent.value
    ? '请填写代验收说明（5~200 字），该说明将作为验收记录留痕'
    : '可选：填写验收意见（5~200 字），留空表示无补充意见'
)

const quickPhrases = computed(() =>
  isAgent.value
    ? ['已超过 48 小时居民未验收，经核实维修已完成，予以代验收']
    : PASS_QUICK_PHRASES
)

function applyPhrase (phrase) {
  form.verifyRemark = phrase
}

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

/** 关闭后清空表单与校验态，避免下次打开残留上一次的内容 */
function handleClosed () {
  form.verifyRemark = ''
  formRef.value?.clearValidate()
}

async function handleSubmit () {
  if (!props.order?.orderId) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const remark = form.verifyRemark.trim()
    // 通过时留空则不传（落库为 null），避免存入空字符串
    const res = isAgent.value
      ? await agentVerifyOrder(props.order.orderId, { verifyRemark: remark })
      : await verifyOrder(props.order.orderId, {
          verifyResult: 1,
          verifyRemark: remark || undefined
        })

    ElMessage.success(res.message || '验收通过')
    emit('update:modelValue', false)
    emit('success', res.data)
  } catch (e) {
    // 403（非本人工单）/ 409（状态不可验收、未达超时阈值）由响应拦截器统一提示
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

.quick-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-phrase {
  cursor: pointer;
}
</style>
