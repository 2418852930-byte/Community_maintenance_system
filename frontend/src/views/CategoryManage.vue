<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 搜索区（C-01 查询条件） ============ -->
      <el-form :model="query" inline>
        <el-form-item label="类目名称">
          <el-input
            v-model="query.categoryName"
            placeholder="类目名称关键字"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="上级类目">
          <el-select v-model="query.parentId" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="item in rootOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- ============ 工具条 ============ -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增类目</el-button>
        <span class="toolbar-tip">
          类目最多两级（BR-1-03）；停用后的类目不会出现在居民报修选择器中（BR-1-05）
        </span>
      </div>

      <!-- ============ 类目列表（C-01） ============ -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="categoryId" label="类目ID" width="90" align="center" />
        <el-table-column prop="categoryName" label="类目名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="上级类目" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.parentId === 0" class="level-root">一级类目</span>
            <span v-else>{{ row.parentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序号" width="90" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <!-- C-07 启用 / 停用，切换前二次确认 -->
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="类目说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无类目数据" />
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

    <!-- ============ 新增 / 编辑类目弹窗（C-04 / C-05） ============ -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑类目' : '新增类目'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级类目" prop="parentId">
          <el-select
            v-model="form.parentId"
            :disabled="parentLocked"
            placeholder="不选则为一级类目"
            style="width: 100%"
          >
            <el-option label="无（作为一级类目）" :value="0" />
            <el-option
              v-for="item in rootOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div v-if="parentLocked" class="form-tip">
            该类目下已存在子类目，不能改为二级类目（否则会出现三级类目，违反 BR-1-03）
          </div>
          <div v-else-if="isEdit && referenced" class="form-tip">
            该类目已被工单使用，仅允许修改名称与排序号，不能变更层级（BR-1-04）
          </div>
        </el-form-item>

        <el-form-item label="类目名称" prop="categoryName">
          <el-input v-model="form.categoryName" maxlength="20" show-word-limit placeholder="2~20 个字符" />
        </el-form-item>

        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 160px" />
          <span class="form-tip" style="margin-left: 8px">值越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="图标路径">
          <el-input v-model="form.icon" placeholder="选填，例如 /images/category/water.png" />
        </el-form-item>

        <el-form-item label="类目说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="选填，说明该类目的适用范围"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  changeCategoryStatus,
  createCategory,
  deleteCategory,
  getCategoryDetail,
  getCategoryTree,
  pageCategories,
  updateCategory
} from '@/api/category'

// ======================= 列表查询（C-01） =======================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryName: '',
  parentId: null,
  status: null
})

async function fetchList () {
  loading.value = true
  try {
    // 过滤空值，避免把空字符串传给后端
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      categoryName: query.categoryName || undefined,
      parentId: query.parentId === null || query.parentId === '' ? undefined : query.parentId,
      status: query.status === null || query.status === '' ? undefined : query.status
    }
    const res = await pageCategories(params)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch () {
  query.pageNum = 1
  fetchList()
}

function handleReset () {
  query.categoryName = ''
  query.parentId = null
  query.status = null
  query.pageNum = 1
  fetchList()
}

// ======================= 类目树：上级类目选项（C-02） =======================
/** 一级类目下拉选项 */
const rootOptions = ref([])
/** 每个类目的子类目数量，用于判断「能否变更层级」 */
const childCountMap = ref({})

async function fetchTree () {
  try {
    // onlyEnabled = false：管理员场景需要看到全部类目（含停用），便于编辑
    const res = await getCategoryTree(false)
    const roots = res.data || []
    rootOptions.value = roots.map((item) => ({ label: item.categoryName, value: item.categoryId }))
    const map = {}
    roots.forEach((item) => {
      map[item.categoryId] = (item.children || []).length
    })
    childCountMap.value = map
  } catch (e) {
    rootOptions.value = []
    childCountMap.value = {}
  }
}

// ======================= 新增 / 编辑弹窗（C-04 / C-05） =======================
const formVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
/** 该类目是否已被工单引用（由详情接口返回的场景标识，仅用于提示文案） */
const referenced = ref(false)

const form = reactive({
  categoryId: null,
  parentId: 0,
  categoryName: '',
  sortOrder: 0,
  status: 1,
  icon: '',
  description: ''
})

/** 该类目下已有子类目时，上级类目不允许改为二级（避免出现三级类目，BR-1-03） */
const parentLocked = computed(() => {
  if (!isEdit.value || !form.categoryId) return false
  return (childCountMap.value[form.categoryId] || 0) > 0
})

const formRules = {
  categoryName: [
    { required: true, message: '请输入类目名称', trigger: 'blur' },
    { min: 2, max: 20, message: '类目名称长度须为 2~20 个字符', trigger: 'blur' }
  ]
}

function openCreateDialog () {
  isEdit.value = false
  referenced.value = false
  form.categoryId = null
  form.parentId = 0
  form.categoryName = ''
  form.sortOrder = 0
  form.status = 1
  form.icon = ''
  form.description = ''
  formVisible.value = true
}

async function openEditDialog (row) {
  try {
    // 拉取详情（C-03），保证编辑表单数据完整
    const res = await getCategoryDetail(row.categoryId)
    const d = res.data
    isEdit.value = true
    referenced.value = false
    Object.assign(form, {
      categoryId: d.categoryId,
      parentId: d.parentId === null || d.parentId === undefined ? 0 : d.parentId,
      categoryName: d.categoryName || '',
      sortOrder: d.sortOrder === null || d.sortOrder === undefined ? 0 : d.sortOrder,
      status: d.status === null || d.status === undefined ? 1 : d.status,
      icon: d.icon || '',
      description: d.description || ''
    })
    formVisible.value = true
  } catch (e) {
    // 错误提示由响应拦截器处理
  }
}

function resetForm () {
  formRef.value?.resetFields()
  referenced.value = false
}

async function handleSubmit () {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      // C-05 修改类目：只提交需要维护的字段；父级锁定时不提交 parentId，
      // 避免触发 BR-1-04 的「已被引用不允许变更层级」校验
      const payload = {
        categoryName: form.categoryName.trim(),
        sortOrder: form.sortOrder,
        status: form.status,
        icon: form.icon || undefined,
        description: form.description || undefined
      }
      if (!parentLocked.value) {
        payload.parentId = form.parentId
      }
      const res = await updateCategory(form.categoryId, payload)
      ElMessage.success(res.message || '保存成功')
    } else {
      // C-04 新增类目
      const payload = {
        categoryName: form.categoryName.trim(),
        parentId: form.parentId,
        sortOrder: form.sortOrder,
        status: form.status,
        icon: form.icon || undefined,
        description: form.description || undefined
      }
      const res = await createCategory(payload)
      ElMessage.success(res.message || '新增成功')
    }
    formVisible.value = false
    await fetchTree()
    if (!isEdit.value) {
      query.pageNum = 1
    }
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理（如「同一上级类目下已存在同名类目」）
  } finally {
    submitLoading.value = false
  }
}

// ======================= 删除类目（C-06） =======================
async function handleDelete (row) {
  try {
    await ElMessageBox.confirm(
      `确认删除类目「${row.categoryName}」吗？删除后不可恢复；若该类目已被工单使用，请改用「停用」。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户取消
  }
  try {
    const res = await deleteCategory(row.categoryId)
    ElMessage.success(res.message || '删除成功')
    await fetchTree()
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理（如「请先删除其下级类目」）
  }
}

// ======================= 启用 / 停用（C-07） =======================
async function handleStatusChange (row) {
  const targetStatus = row.status
  const actionText = targetStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}类目「${row.categoryName}」吗？` +
        (targetStatus === 0 ? '停用后居民报修页将不再展示该选项。' : ''),
      '操作确认',
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: 'warning' }
    )
    const res = await changeCategoryStatus(row.categoryId, targetStatus)
    ElMessage.success(res.message || `${actionText}成功`)
    fetchList()
  } catch (e) {
    // 用户取消或接口失败：回滚开关状态
    row.status = targetStatus === 1 ? 0 : 1
  }
}

onMounted(async () => {
  await fetchTree()
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

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}

.level-root {
  color: #909399;
  font-size: 12px;
}
</style>
