<template>
  <div class="page-container">
    <!-- 驳回提示区：仅在「修改后重新提交」模式下展示，对应 FR-2-05 驳回原因展示 -->
    <el-alert
      v-if="isResubmit"
      type="error"
      :closable="false"
      show-icon
      class="reject-alert"
    >
      <template #title>该工单已被管理员驳回，请根据驳回意见修改后重新提交</template>
      <div class="reject-reason">
        <span class="reject-label">驳回原因：</span>{{ order.rejectReason || '（未填写）' }}
      </div>
      <div v-if="order.rejectorName || order.rejectTime" class="reject-meta">
        驳回人：{{ order.rejectorName || '-' }}　驳回时间：{{ order.rejectTime || '-' }}
      </div>
    </el-alert>

    <div class="page-card">
      <div class="card-header">
        <span class="card-title">{{ pageTitle }}</span>
        <span class="card-tip">
          提交后工单状态为「待审核」，需经管理员审核通过后才进入派单流程
        </span>
      </div>

      <el-form
        ref="formRef"
        v-loading="loading"
        :model="form"
        :rules="rules"
        label-width="110px"
        style="max-width: 760px"
      >
        <!-- 类目：数据源为模块一 C-02 类目树，onlyEnabled=true 保证停用类目不可选（BR-1-05） -->
        <el-form-item label="报修类目" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryTree"
            :props="cascaderProps"
            placeholder="请选择报修类目（可只选一级类目）"
            clearable
            style="width: 100%"
          />
          <div class="form-tip">类目分两级，也可直接选择一级类目；停用类目不会出现在下拉中</div>
        </el-form-item>

        <el-form-item label="报修标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="简要描述故障，如：厨房水管漏水"
          />
        </el-form-item>

        <el-form-item label="故障描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述故障现象、发生位置与出现时间（10~500 字）"
          />
        </el-form-item>

        <!-- 现场照片：最多 3 张，单张 ≤ 5MB，走公共接口 U-15 -->
        <el-form-item label="现场照片">
          <div class="upload-wrap">
            <el-upload
              v-model:file-list="fileList"
              list-type="picture-card"
              :limit="3"
              accept=".jpg,.jpeg,.png"
              :http-request="handleUpload"
              :before-upload="beforeUpload"
              :on-exceed="handleExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="form-tip">选填，最多 3 张，单张不超过 5MB，仅支持 jpg / jpeg / png</div>
          </div>
        </el-form-item>

        <el-form-item label="维修地址" prop="address">
          <el-input
            v-model="form.address"
            maxlength="255"
            placeholder="默认取您账号中的楼栋房号，可修改"
          />
          <div class="form-tip">默认自动带出「个人中心」中登记的居住地址，可直接修改</div>
        </el-form-item>

        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" maxlength="50" placeholder="2~50 个字符" />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>

        <el-form-item label="紧急程度" prop="urgency">
          <el-radio-group v-model="form.urgency">
            <el-radio :value="0">普通（按提交顺序派单）</el-radio>
            <el-radio :value="1">紧急（待派单池中置顶）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="期望上门时间">
          <el-date-picker
            v-model="form.expectTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选填，不得早于当前时间"
            :disabled-date="disabledDate"
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitText }}
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryTree } from '@/api/category'
import { createOrder, getOrderDetail, resubmitOrder, updateOrder } from '@/api/order'
import { uploadImage } from '@/api/file'
import { useUserStore } from '@/store/user'

/**
 * 报修工单提交页（模块二）
 *
 * 覆盖三个接口：
 *  - O-01 提交报修工单（默认模式）
 *  - O-04 修改工单（?orderId=xx&mode=edit）
 *  - O-06 驳回后修改重新提交（?orderId=xx&mode=resubmit）
 *
 * 三个接口的字段校验规则完全一致，因此共用同一套表单，
 * 仅「提交时调用的接口」与「成功后的提示文案」不同 —— 避免为一个差异再复制一个页面。
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ======================= 模式判定 =======================
const orderId = computed(() => route.query.orderId || null)
const isResubmit = computed(() => route.query.mode === 'resubmit')
const isEdit = computed(() => !!orderId.value && !isResubmit.value)
const isModifyMode = computed(() => !!orderId.value)

const pageTitle = computed(() => {
  if (isResubmit.value) return '修改并重新提交报修工单'
  if (isEdit.value) return '修改报修工单'
  return '提交报修'
})
const submitText = computed(() => {
  if (isResubmit.value) return '重新提交'
  if (isEdit.value) return '保存修改'
  return '提交报修'
})

// ======================= 表单 =======================
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  categoryId: null,
  title: '',
  description: '',
  address: '',
  contactName: '',
  contactPhone: '',
  urgency: 0,
  expectTime: ''
})

/** 详情数据（编辑 / 重新提交模式下用于展示驳回信息） */
const order = ref({})

const rules = {
  categoryId: [{ required: true, message: '请选择报修类目', trigger: 'change' }],
  title: [
    { required: true, message: '请填写报修标题', trigger: 'blur' },
    { min: 2, max: 100, message: '报修标题长度须为 2~100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请填写故障详细描述', trigger: 'blur' },
    { min: 10, max: 500, message: '故障描述长度需在 10~500 字之间', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请填写上门维修地址', trigger: 'blur' },
    { min: 2, max: 255, message: '上门维修地址长度须为 2~255 个字符', trigger: 'blur' }
  ],
  contactName: [
    { required: true, message: '请填写联系人姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '联系人姓名长度须为 2~50 个字符', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请填写联系人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ]
}

/** 不可选择早于今天的日期（更精确的「不得早于当前时间」由后端 BR-2-08 校验） */
function disabledDate (date) {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

// ======================= 类目级联（复用模块一 C-02） =======================
const categoryTree = ref([])
/** checkStrictly 允许只选一级类目；emitPath=false 时 v-model 直接绑定 categoryId */
const cascaderProps = {
  value: 'categoryId',
  label: 'categoryName',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

async function fetchCategoryTree () {
  try {
    // onlyEnabled = true：停用类目不可被新工单选择（BR-1-05）
    const res = await getCategoryTree(true)
    categoryTree.value = res.data || []
  } catch (e) {
    categoryTree.value = []
  }
}

// ======================= 图片上传（公共接口 U-15） =======================
const fileList = ref([])

/** 上传前本地校验：类型与大小，减少无效请求 */
function beforeUpload (file) {
  const name = (file.name || '').toLowerCase()
  const okType = /\.(jpg|jpeg|png)$/.test(name)
  if (!okType) {
    ElMessage.error('仅支持 jpg / jpeg / png 格式的图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
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
  ElMessage.warning('现场照片最多上传 3 张')
}

/** 提交时把已上传成功的图片路径按英文逗号拼接（对应 repair_order.images 字段口径） */
function buildImages () {
  return fileList.value
    .map((item) => item.url)
    .filter((url) => !!url)
    .join(',')
}

// ======================= 数据加载 =======================
async function loadOrder () {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    const d = res.data
    order.value = d
    Object.assign(form, {
      categoryId: d.categoryId ?? null,
      title: d.title || '',
      description: d.description || '',
      address: d.address || '',
      contactName: d.contactName || '',
      contactPhone: d.contactPhone || '',
      urgency: d.urgency ?? 0,
      expectTime: d.expectTime || ''
    })
    fileList.value = (d.images ? d.images.split(',') : [])
      .filter((url) => !!url)
      .map((url, index) => ({ name: `现场照片${index + 1}`, url }))
  } catch (e) {
    // 无权限 / 工单不存在时返回列表页
    router.replace('/my-orders')
  } finally {
    loading.value = false
  }
}

/** 新建模式下用当前登录居民的资料预填地址与联系人（BR-8-05 / BR-2-02） */
function prefillFromProfile () {
  const info = userStore.userInfo || {}
  form.address = info.address || ''
  form.contactName = info.realName || ''
  form.contactPhone = info.phone || ''
}

// ======================= 提交 =======================
async function handleSubmit () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    categoryId: form.categoryId,
    title: form.title.trim(),
    description: form.description.trim(),
    address: form.address.trim(),
    contactName: form.contactName.trim(),
    contactPhone: form.contactPhone.trim(),
    urgency: form.urgency,
    // 未上传照片 / 未填时间时不传该字段，避免把空值写库
    images: buildImages() || undefined,
    expectTime: form.expectTime || undefined
  }

  submitting.value = true
  try {
    let res
    if (isResubmit.value) {
      res = await resubmitOrder(orderId.value, payload)
    } else if (isEdit.value) {
      res = await updateOrder(orderId.value, payload)
    } else {
      res = await createOrder(payload)
    }
    ElMessage.success(res.message || '操作成功')
    const targetId = res.data && res.data.orderId ? res.data.orderId : orderId.value
    router.replace(`/orders/${targetId}`)
  } catch (e) {
    // 错误提示由响应拦截器统一处理（400 字段明细 / 403 越权 / 409 状态冲突）
  } finally {
    submitting.value = false
  }
}

function handleCancel () {
  if (isModifyMode.value) {
    router.back()
  } else {
    router.push('/my-orders')
  }
}

onMounted(async () => {
  await fetchCategoryTree()
  if (isModifyMode.value) {
    await loadOrder()
  } else {
    prefillFromProfile()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.card-tip {
  font-size: 12px;
  color: #64748b;
}

.form-tip {
  font-size: 12px;
  color: #64748b;
  line-height: 18px;
}

.upload-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reject-alert {
  margin-bottom: 12px;
}

.reject-reason {
  margin-top: 6px;
  font-size: 13px;
  line-height: 20px;
}

.reject-label {
  font-weight: 600;
}

.reject-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}
</style>
