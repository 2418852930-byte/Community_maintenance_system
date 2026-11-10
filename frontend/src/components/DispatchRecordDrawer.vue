<template>
  <el-drawer
    :model-value="modelValue"
    :title="drawerTitle"
    size="540px"
    @update:model-value="handleVisibleChange"
    @open="fetchRecords"
  >
    <div v-loading="loading">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        数据来源为派单记录表，按操作时间升序展示该工单的完整派单轨迹；
        每次派单 / 改派 / 发布抢单池 / 抢单 / 接单 / 拒单均独立留痕，审核环节不计入本轨迹。
      </el-alert>

      <el-empty
        v-if="!loading && records.length === 0"
        description="该工单暂无派单记录（尚未进入派单链路）"
      />

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in records"
          :key="item.dispatchId"
          :timestamp="item.dispatchTime"
          placement="top"
          :type="timelineType(item.dispatchType)"
        >
          <div class="record-head">
            <el-tag :type="dispatchTypeTag(item.dispatchType)" size="small" effect="dark">
              {{ item.dispatchTypeName }}
            </el-tag>
            <el-tag :type="item.opResult === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ item.opResultName }}
            </el-tag>
          </div>

          <div class="record-text">{{ dispatchRecordText(item) }}</div>

          <div class="record-meta">
            操作人：{{ item.operatorName || '-' }}（{{ roleName(item.operatorRole) }}）
          </div>

          <div v-if="item.remark" class="record-remark">备注：{{ item.remark }}</div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { listDispatchRecords } from '@/api/dispatch'
import { dispatchRecordText, dispatchTypeTag } from '@/constants/dispatch'

/**
 * 派单轨迹抽屉（模块三 D-10 派单记录查询）
 *
 * 通用公共组件：管理员派单工作台（OrderDispatch.vue）与维修人员工单大厅（GrabHall.vue）
 * 共用本组件，避免在两个页面各写一份轨迹渲染逻辑。
 *
 * 权限由服务端保证：管理员可查任意工单；维修人员仅可查本人受派工单，越权返回 403。
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

const drawerTitle = computed(() => (props.orderNo ? `派单轨迹 · ${props.orderNo}` : '派单轨迹'))

/** 角色编码 -> 中文名（接口 D-10 出参 operatorRole 为字符编码） */
const ROLE_NAME = {
  resident: '居民',
  repair: '维修人员',
  admin: '系统管理员'
}

function roleName (role) {
  return ROLE_NAME[role] || role || '-'
}

/** 时间线节点颜色：成功推进为绿色，回退（拒单）为红色，发布抢单池为灰色 */
function timelineType (dispatchType) {
  if (dispatchType === 6) return 'danger'
  if (dispatchType === 3) return 'info'
  return 'success'
}

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

async function fetchRecords () {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await listDispatchRecords(props.orderId)
    records.value = res.data || []
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.record-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.record-text {
  font-size: 14px;
  color: #303133;
}

.record-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.record-remark {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
</style>
