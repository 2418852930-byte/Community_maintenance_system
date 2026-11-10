<template>
  <div class="page-container">
    <div class="page-card">
      <!-- ============ 搜索区（U-08 查询条件） ============ -->
      <el-form :model="query" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="用户名 / 姓名 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- ============ 工具条 ============ -->
      <div class="toolbar">
        <!-- U-10 新增用户：入口只提供维修人员 / 系统管理员（BR-8-16） -->
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
        <span class="toolbar-tip">居民账号请由居民自助注册，本入口仅创建维修人员与系统管理员</span>
      </div>

      <!-- ============ 用户列表（U-08） ============ -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="{ row }">
            <!-- U-13 启用 / 禁用，切换前二次确认 -->
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="row.userId === currentUserId"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="锁定" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.locked" type="warning" size="small">锁定中</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailDrawer(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="openRoleDialog(row)">角色</el-button>
            <el-button link type="warning" size="small" @click="openResetPwdDialog(row)">重置密码</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无用户数据" />
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

    <!-- ============ 新增用户对话框（U-10） ============ -->
    <el-dialog v-model="createVisible" title="新增用户" width="560px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="createForm.role">
            <el-radio value="repair">维修人员</el-radio>
            <el-radio value="admin">系统管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="4~20 位字母、数字或下划线" />
        </el-form-item>

        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="6~20 位，须同时包含字母和数字" />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="2~10 个字符" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" maxlength="11" placeholder="11 位手机号，全局唯一" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="createForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="选填" />
        </el-form-item>

        <!-- 维修人员专属字段（BR-8-15：工种为选填） -->
        <template v-if="createForm.role === 'repair'">
          <el-form-item label="维修工种">
            <el-select v-model="createForm.workerType" placeholder="选填" clearable style="width: 100%">
              <el-option v-for="item in workerTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="技能描述">
            <el-input v-model="createForm.skillDesc" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>

          <el-form-item label="工作状态">
            <el-radio-group v-model="createForm.workStatus">
              <el-radio :value="0">休息</el-radio>
              <el-radio :value="1">空闲</el-radio>
              <el-radio :value="2">忙碌</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 编辑用户对话框（U-11） ============ -->
    <el-dialog v-model="editVisible" title="编辑用户信息" width="560px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="登录账号">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input :model-value="editForm.phone" disabled />
          <div class="form-tip">手机号与创建时间不可修改（BR-8-14）</div>
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="2~10 个字符" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="选填" />
        </el-form-item>

        <template v-if="editForm.role === 'repair'">
          <el-form-item label="维修工种">
            <el-select v-model="editForm.workerType" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in workerTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="技能描述">
            <el-input v-model="editForm.skillDesc" type="textarea" :rows="2" />
          </el-form-item>

          <el-form-item label="工作状态">
            <el-radio-group v-model="editForm.workStatus">
              <el-radio :value="0">休息</el-radio>
              <el-radio :value="1">空闲</el-radio>
              <el-radio :value="2">忙碌</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <el-form-item v-if="editForm.role === 'resident'" label="楼栋房号">
          <el-input v-model="editForm.address" placeholder="居民楼栋房号" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 角色变更对话框（U-12） ============ -->
    <el-dialog v-model="roleVisible" title="角色分配与变更" width="440px">
      <el-alert
        title="角色变更在下一次请求即生效，用户重新登录后菜单同步刷新（BR-8-17）"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form label-width="90px">
        <el-form-item label="用户">
          <el-input :model-value="`${roleForm.realName}（${roleForm.username}）`" disabled />
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag :type="roleTagType(roleForm.oldRole)" size="small">{{ roleNameOf(roleForm.oldRole) }}</el-tag>
        </el-form-item>
        <el-form-item label="目标角色">
          <el-select v-model="roleForm.role" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="handleChangeRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 重置密码对话框（U-14） ============ -->
    <el-dialog v-model="pwdVisible" title="重置用户密码" width="460px" @closed="resetPwdFormData">
      <el-alert
        :title="`即将重置「${pwdForm.realName}（${pwdForm.username}）」的登录密码`"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6~20 位，须同时包含字母和数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleResetPassword">确定重置</el-button>
      </template>
    </el-dialog>

    <!-- ============ 用户详情抽屉（U-09） ============ -->
    <el-drawer v-model="detailVisible" title="用户详情" size="420px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detail.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ detail.roleName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderName(detail.gender) }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="电子邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="楼栋房号">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修工种">{{ detail.workerType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="技能描述">{{ detail.skillDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作状态">{{ workStatusName(detail.workStatus) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">{{ detail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ detail.lastLoginTime || '从未登录' }}</el-descriptions-item>
        <el-descriptions-item label="连续失败次数">{{ detail.loginFailCount }}</el-descriptions-item>
        <el-descriptions-item label="锁定截止">{{ detail.lockUntil || '未锁定' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  changeRole,
  changeStatus,
  createUser,
  getUserDetail,
  pageUsers,
  resetPassword,
  updateUser
} from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentUserId = computed(() => (userStore.userInfo ? userStore.userInfo.userId : null))

const roleOptions = [
  { label: '居民', value: 'resident' },
  { label: '维修人员', value: 'repair' },
  { label: '系统管理员', value: 'admin' }
]

/** 与需求文档 5.8.5 节保持一致 */
const workerTypes = ['水电', '门窗', '公共照明', '给排水', '综合']

// ======================= 列表查询 =======================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  role: '',
  status: null
})

async function fetchList () {
  loading.value = true
  try {
    // 过滤掉空值，避免把空字符串传给后端
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    }
    const res = await pageUsers(params)
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
  query.keyword = ''
  query.role = ''
  query.status = null
  query.pageNum = 1
  fetchList()
}

// ======================= 展示辅助 =======================
function roleTagType (role) {
  if (role === 'admin') return 'danger'
  if (role === 'repair') return 'warning'
  return 'success'
}

function roleNameOf (role) {
  const found = roleOptions.find((item) => item.value === role)
  return found ? found.label : role
}

function genderName (gender) {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '未知'
}

function workStatusName (workStatus) {
  if (workStatus === 0) return '休息'
  if (workStatus === 1) return '空闲'
  if (workStatus === 2) return '忙碌'
  return '-'
}

// ======================= U-10 新增用户 =======================
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref(null)

const createForm = reactive({
  role: 'repair',
  username: '',
  password: '',
  realName: '',
  phone: '',
  gender: 0,
  email: '',
  workerType: '',
  skillDesc: '',
  workStatus: 1,
  remark: ''
})

const createRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '用户名需为 4~20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/, message: '密码需为 6~20 位且同时包含字母和数字', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '真实姓名长度需为 2~10 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreateDialog () {
  createVisible.value = true
}

function resetCreateForm () {
  createFormRef.value?.resetFields()
  Object.assign(createForm, {
    role: 'repair',
    username: '',
    password: '',
    realName: '',
    phone: '',
    gender: 0,
    email: '',
    workerType: '',
    skillDesc: '',
    workStatus: 1,
    remark: ''
  })
}

async function handleCreate () {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  createLoading.value = true
  try {
    // 只提交后端允许的字段，避免多余字段造成校验噪音
    const payload = {
      role: createForm.role,
      username: createForm.username.trim(),
      password: createForm.password,
      realName: createForm.realName,
      phone: createForm.phone.trim(),
      gender: createForm.gender,
      email: createForm.email || undefined,
      remark: createForm.remark || undefined
    }
    if (createForm.role === 'repair') {
      payload.workerType = createForm.workerType || undefined
      payload.skillDesc = createForm.skillDesc || undefined
      payload.workStatus = createForm.workStatus
    }
    const res = await createUser(payload)
    ElMessage.success(res.message || '新增成功')
    createVisible.value = false
    query.pageNum = 1
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理
  } finally {
    createLoading.value = false
  }
}

// ======================= U-11 修改用户 =======================
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)

const editForm = reactive({
  userId: null,
  username: '',
  phone: '',
  role: '',
  realName: '',
  gender: 0,
  email: '',
  address: '',
  workerType: '',
  skillDesc: '',
  workStatus: 1,
  remark: ''
})

const editRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '真实姓名长度需为 2~10 个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function openEditDialog (row) {
  try {
    // 拉取详情（U-09），保证编辑表单数据完整
    const res = await getUserDetail(row.userId)
    const d = res.data
    Object.assign(editForm, {
      userId: d.userId,
      username: d.username,
      phone: d.phone,
      role: d.role,
      realName: d.realName || '',
      gender: d.gender === null || d.gender === undefined ? 0 : d.gender,
      email: d.email || '',
      address: d.address || '',
      workerType: d.workerType || '',
      skillDesc: d.skillDesc || '',
      workStatus: d.workStatus === null || d.workStatus === undefined ? 1 : d.workStatus,
      remark: d.remark || ''
    })
    editVisible.value = true
  } catch (e) {
    // 错误提示由响应拦截器处理
  }
}

async function handleEdit () {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  editLoading.value = true
  try {
    // 注意：不要提交 phone / username / createTime，后端会直接返回 400（BR-8-14）
    const payload = {
      realName: editForm.realName,
      gender: editForm.gender,
      email: editForm.email || undefined,
      remark: editForm.remark || undefined
    }
    if (editForm.role === 'repair') {
      payload.workerType = editForm.workerType || undefined
      payload.skillDesc = editForm.skillDesc || undefined
      payload.workStatus = editForm.workStatus
    }
    if (editForm.role === 'resident') {
      payload.address = editForm.address || undefined
    }
    const res = await updateUser(editForm.userId, payload)
    ElMessage.success(res.message || '保存成功')
    editVisible.value = false
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理
  } finally {
    editLoading.value = false
  }
}

// ======================= U-12 角色变更 =======================
const roleVisible = ref(false)
const roleLoading = ref(false)
const roleForm = reactive({
  userId: null,
  username: '',
  realName: '',
  oldRole: '',
  role: ''
})

function openRoleDialog (row) {
  Object.assign(roleForm, {
    userId: row.userId,
    username: row.username,
    realName: row.realName,
    oldRole: row.role,
    role: row.role
  })
  roleVisible.value = true
}

async function handleChangeRole () {
  if (roleForm.role === roleForm.oldRole) {
    ElMessage.warning('目标角色与当前角色相同')
    return
  }
  roleLoading.value = true
  try {
    const res = await changeRole(roleForm.userId, roleForm.role)
    ElMessage.success(res.message || '角色变更成功')
    roleVisible.value = false
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理（如「系统须至少保留一个启用的管理员账号」）
  } finally {
    roleLoading.value = false
  }
}

// ======================= U-13 启用 / 禁用 =======================
async function handleStatusChange (row) {
  const targetStatus = row.status
  const actionText = targetStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}账号「${row.realName}（${row.username}）」吗？`,
      '操作确认',
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: 'warning' }
    )
    const res = await changeStatus(row.userId, targetStatus)
    ElMessage.success(res.message || `${actionText}成功`)
    // 后端可能返回提示语（例如禁用维修人员后需到派单模块改派）
    if (res.data && res.data.tip) {
      ElMessage.info(res.data.tip)
    }
    fetchList()
  } catch (e) {
    // 用户取消或接口失败：回滚开关状态
    row.status = targetStatus === 1 ? 0 : 1
  }
}

// ======================= U-14 重置密码 =======================
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({
  userId: null,
  username: '',
  realName: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/, message: '密码需为 6~20 位且同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function openResetPwdDialog (row) {
  Object.assign(pwdForm, {
    userId: row.userId,
    username: row.username,
    realName: row.realName,
    newPassword: '',
    confirmPassword: ''
  })
  pwdVisible.value = true
}

function resetPwdFormData () {
  pwdFormRef.value?.resetFields()
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

async function handleResetPassword () {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    const res = await resetPassword(pwdForm.userId, {
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success(res.message || '密码重置成功')
    pwdVisible.value = false
    fetchList()
  } catch (e) {
    // 错误提示由响应拦截器处理
  } finally {
    pwdLoading.value = false
  }
}

// ======================= U-09 用户详情 =======================
const detailVisible = ref(false)
const detail = ref(null)

async function openDetailDrawer (row) {
  try {
    const res = await getUserDetail(row.userId)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    // 错误提示由响应拦截器处理
  }
}

onMounted(fetchList)
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
</style>
