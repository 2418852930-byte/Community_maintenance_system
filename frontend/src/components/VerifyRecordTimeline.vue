<template>
  <el-drawer
    :model-value="modelValue"
    :title="drawerTitle"
    size="560px"
    @update:model-value="handleVisibleChange"
    @open="fetchRecords"
  >
    <div v-loading="loading">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        本时间轴数据来源为<b>验收记录表</b>（<code>repair_verify_log</code>），
        按验收轮次升序展示该工单的<b>全部</b>验收过程。每轮验收独立成行、一经写入即不可修改，
        因此「多次驳回 → 重新维修 → 再次验收」的完整过程全程可追溯（BR-5-05）。
      </el-alert>

      <el-empty
        v-if="!loading && records.length === 0"
        description="该工单暂无验收记录（尚未进入验收环节）"
      />

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in records"
          :key="item.verifyId"
          :timestamp="item.verifyTime"
          placement="top"
          :type="verifyResultTimeline(item.verifyResult)"
        >
          <div class="record-head">
            <el-tag type="info" size="small" effect="plain">第 {{ item.verifyRound }} 轮</el-tag>
            <el-tag :type="verifyTypeTag(item.verifyType)" size="small" effect="dark">
              {{ item.verifyTypeName || verifyTypeName(item.verifyType) }}
            </el-tag>
            <el-tag :type="verifyResultTag(item.verifyResult)" size="small" effect="dark">
              {{ item.verifyResultName || verifyResultName(item.verifyResult) }}
            </el-tag>
          </div>

          <div class="record-operator">
            验收人：{{ item.verifierName || '-' }}
            <span v-if="item.verifierRole" class="role-text">（{{ roleName(item.verifierRole) }}）</span>
          </div>

          <div v-if="item.verifyRemark" class="record-content">{{ item.verifyRemark }}</div>
          <div v-else class="record-content empty-remark">本次验收未填写意见</div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { listVerifyRecords } from '@/api/verify'
import {
  verifyResultName,
  verifyResultTag,
  verifyResultTimeline,
  verifyTypeName,
  verifyTypeTag
} from '@/constants/verify'
import { ROLE_NAMES } from '@/store/user'

/**
 * 验收记录时间轴抽屉（模块五 V-04 验收记录查询）
 *
 * 公共展示组件：待验收工单列表页的「验收记录」按钮进入，
 * 与模块四的 OrderRecordTimeline（维修进度时间轴）形成互补 ——
 * 前者看「修的过程」，本组件看「验的过程」。
 *
 * 权限由服务端保证：居民仅可查本人工单、维修人员仅可查本人受派工单、管理员可查全部，
 * 越权返回 403。
 */
const props = defineProps({
  /** 抽屉显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 工单 ID */
  orderId: { type: [Number, String], default: null },
  /** 工单编号，仅用于标题展示 */
  orderNo: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const records = ref([])

const drawerTitle = computed(() =>
  props.orderNo ? `验收记录 · ${props.orderNo}` : '验收记录'
)

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

/** 复用 store 中的角色字典，避免前端出现第二份角色中文名表 */
function roleName (role) {
  return ROLE_NAMES[role] || role
}

async function fetchRecords () {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await listVerifyRecords(props.orderId)
    records.value = res.data || []
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

defineExpose({ fetchRecords })
</script>

<style scoped>
.record-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.record-operator {
  font-size: 13px;
  color: #606266;
}

.role-text {
  font-size: 12px;
  color: #909399;
}

.record-content {
  margin-top: 4px;
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
  line-height: 1.6;
}

.empty-remark {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
