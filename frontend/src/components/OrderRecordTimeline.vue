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
        本时间轴数据来源为<b>维修记录表</b>（<code>repair_record</code>），按记录发生时间升序展示
        该工单的全部进度节点。记录<b>一经写入即不可修改、不可删除</b>（BR-4-05），
        因此维修过程全程可追溯。
      </el-alert>

      <el-form inline size="small" style="margin-bottom: 8px">
        <el-form-item label="筛选类型">
          <el-select
            v-model="recordType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
            @change="fetchRecords"
          >
            <el-option
              v-for="(label, value) in RECORD_TYPE"
              :key="value"
              :label="label"
              :value="Number(value)"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-empty
        v-if="!loading && records.length === 0"
        description="该工单暂无维修记录（尚未进入维修链路）"
      />

      <el-timeline v-else>
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
            <span class="record-operator">{{ item.workerName || '-' }}</span>
          </div>

          <div class="record-text">{{ recordText(item) }}</div>
          <div class="record-content">{{ item.progressContent }}</div>

          <div v-if="item.partsUsed" class="record-extra">配件：{{ item.partsUsed }}</div>
          <div v-if="item.cost !== null && item.cost !== undefined" class="record-extra">
            本次费用：<span class="cost">¥ {{ Number(item.cost).toFixed(2) }}</span>
          </div>

          <div v-if="imageList(item.images).length" class="image-row">
            <el-image
              v-for="(img, idx) in imageList(item.images)"
              :key="idx"
              :src="img"
              :preview-src-list="imageList(item.images)"
              :initial-index="idx"
              fit="cover"
              class="preview-img"
              preview-teleported
            />
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { listOrderRecords } from '@/api/progress'
import { RECORD_TYPE, recordText, recordTypeName, recordTypeTag, recordTypeTimeline } from '@/constants/progress'

/**
 * 维修进度时间轴抽屉（模块四 R-01 进度时间线查询）
 *
 * 公共展示组件：「我的工单列表」与「维修记录填写页」共用本组件，
 * 避免在两个页面各写一份时间轴渲染逻辑（与模块三 DispatchRecordDrawer 的组织方式一致）。
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
const recordType = ref(null)

const drawerTitle = computed(() => (props.orderNo ? `维修进度 · ${props.orderNo}` : '维修进度'))

function handleVisibleChange (visible) {
  emit('update:modelValue', visible)
}

/** images 字段为逗号分隔路径，拆分为图片数组供 el-image 预览 */
function imageList (images) {
  if (!images) return []
  return String(images).split(',').map((s) => s.trim()).filter(Boolean)
}

async function fetchRecords () {
  if (!props.orderId) return
  loading.value = true
  try {
    // R-01：recordType 为可选的类型筛选
    const res = await listOrderRecords(props.orderId, recordType.value)
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
  gap: 8px;
  margin-bottom: 4px;
}

.record-operator {
  font-size: 12px;
  color: #909399;
}

.record-text {
  font-size: 13px;
  color: #606266;
}

.record-content {
  margin-top: 4px;
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

.cost {
  color: #e6a23c;
  font-weight: 600;
}

.image-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.preview-img {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
