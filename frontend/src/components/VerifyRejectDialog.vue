<template>
  <el-dialog
    :model-value="modelValue"
    title="验收驳回（返修）"
    width="560px"
    @update:model-value="handleVisibleChange"
    @closed="handleClosed"
  >
    <el-alert type="error" :closable="false" show-icon style="margin-bottom: 12px">
      驳回后工单状态将由 <b>待验收(4)</b> 回退为 <b>维修中(3)</b>，重新流入维修进度模块；
      原受派维修人员<b>保持不变</b>，由同一名维修人员返修后再次提交验收（BR-5-03 / BR-5-04）。
      请务必填写具体的驳回原因，维修人员将据此返工。
    </el-alert>

    <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
      <el-descriptions-item label="工单编号">{{ order?.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修类目">{{ order?.categoryName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="报修标题" :span="2">{{ order?.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="维修人员">{{ order?.workerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="完工时间">{{ order?.finishTime || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="驳回原因" prop="verifyRemark">
        <el-input
          v-model="form.verifyRemark"
          type="textarea"
          :rows="4"
          :maxlength="VERIFY_REMARK_MAX"
          show-word-limit
          placeholder="请说明维修存在什么问题（5~200 字），例如：水龙头仍有渗水，请重新处理"
        />
      </el-form-item>

      <el-form-item label="快捷短语">
        <div class="quick-phrases">
          <el-tag
            v-for="(phrase, idx) in REJECT_QUICK_PHRASES"
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
      <el-button type="danger" :loading="loading" @click="handleSubmit">
        确认驳回并返修
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { verifyOrder } from '@/api/verify'
import { REJECT_QUICK_PHRASES, VERIFY_REMARK_MAX, VERIFY_REMARK_MIN } from '@/constants/verify'

/**
 * 驳回返修弹窗（模块五 V-02 的「验收驳回」分支，verifyResult = 0）
 *
 * 与「验收确认弹窗」共用同一个后端接口（V-02），差别在于本组件
 * 把「驳回原因」设为<b>必填且 5~200 字</b>（BR-5-03）——
 * 驳回而不说明原因，维修人员无从下手返工，因此这是质量门禁而非可选提示。
 *
 * 驳回成功后工单回到「维修中(3)」，由原维修人员在模块四继续处理；
 * 维修人员再次提交完工后，本工单会重新出现在待验收列表中（BR-5-04 / BR-5-05）。
 */
const props = defineProps({
  /** 弹窗显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 目标工单（列表行对象） */
  order: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref(null)
const loading = ref(false)

const form = reactive({ verifyRemark: '' })

const rules = {
  verifyRemark: [
    { required: true, message: '请填写驳回原因', trigger: 'blur' },
    {
      min: VERIFY_REMARK_MIN,
      max: VERIFY_REMARK_MAX,
      message: '驳回原因长度须为 5~200 个字符',
      trigger: 'blur'
    }
  ]
}

function applyPhrase (phrase) {
  form.verifyRemark = phrase
}

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

/** 关闭后清空表单与校验态，避免下次打开残留上一次的驳回原因 */
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
    // verifyResult = 0 即驳回返修分支（与「验收通过」同一个 V-02 接口）
    const res = await verifyOrder(props.order.orderId, {
      verifyResult: 0,
      verifyRemark: form.verifyRemark.trim()
    })
    ElMessage.success(res.message || '已驳回，工单返回维修中')
    emit('update:modelValue', false)
    emit('success', res.data)
  } catch (e) {
    // 403（非本人工单）/ 409（状态不可验收）由响应拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.quick-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-phrase {
  cursor: pointer;
}
</style>
