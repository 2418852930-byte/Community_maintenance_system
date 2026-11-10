import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore, ROLE_HOME } from '@/store/user'
import MainLayout from '@/layout/MainLayout.vue'

/**
 * 路由表与「角色菜单隔离」。
 *
 * 隔离方式有双保险：
 *  1. 路由级：meta.roles 声明允许访问的角色，由全局前置守卫拦截；
 *  2. 菜单级：MainLayout 依据同一份 meta.roles 过滤侧边菜单，不展示无权访问的项。
 *
 * 模块二新增的 meta.hidden 约定：标记为 hidden 的子路由仍参与角色校验，
 * 但不会渲染为侧边菜单项（适用于「详情页 / 表单页」这类只能由列表页进入的页面）。
 *
 * 模块三（工单派单调度）新增两条管理员菜单 —— 「工单审核」（OrderAudit）与
 * 「派单调度」（OrderDispatch），以及一条维修人员菜单 —— 「工单大厅」（GrabHall）。
 * 三者均为独立菜单页（用户已确认「两个独立管理员菜单页」方案），
 * 分别对应「审核（8 → 0/9）」与「派单（0 → 1）」两级管理员工作台，以及维修人员的抢单接单入口。
 *
 * 模块四（维修进度管理）把原有的「我的工单」占位页（Placeholder）替换为真实页面
 * MyTasks.vue —— 维修人员在此查看本人受派工单、接单、开始维修、更新进度、提交完工；
 * 并新增一条 hidden 子路由「维修记录填写页」（RepairRecordForm，R-02 / R-04），
 * 只能由「我的工单」列表页的行内按钮进入，故不渲染为侧边菜单项。
 *
 * 模块五（工单验收管理）新增一条菜单路由「工单验收」（OrderVerify，V-01 ~ V-04）——
 * 居民在此查看本人提交的待验收工单、验收通过或驳回返修；管理员在此查看全部待验收工单，
 * 并对超时未验收的工单执行代验收。同一页面按角色呈现不同操作，
 * 故 roles 同时放开给 resident 与 admin（维修人员不可访问，菜单亦不展示）；
 * 该页为新增页面，未触碰任何既有路由条目。
 *
 * 模块六（服务评价管理）新增两条菜单路由：
 *  1. 「服务评价」（MyEvaluations，E-01 ~ E-03）—— 承接模块五流转的「已验收(5)」工单。
 *     居民在此看到「待评价」Tab（待评价列表复用模块二 O-02 + status=5）与「我的评价」Tab；
 *     维修人员在此查看本人所接工单收到的评价（FR-6-07，服务端按 worker_id 限定），
 *     故 roles 同时放开给 resident 与 repair（管理员另有独立的评价管理页）；
 *  2. 「评价管理」（EvaluationManage，E-02 ~ E-04）—— 仅系统管理员，
 *     查看全部评价并按评分 / 类目 / 维修人员 / 回复状态筛选、对评价进行一次性回复。
 * 两条均为新增页面，未触碰任何既有路由条目。
 *
 * 再次强调：前端隔离只是用户体验优化，服务端 JwtInterceptor 才是权威校验。
 *
 * 模块七（报修统计分析）新增一条菜单路由「统计分析」（StatisticsBoard，S-01 ~ S-05）——
 * 数据汇总模块，位于业务链路最下游：上游承接模块二（提交）~ 模块六（评价）产生的
 * 全部业务数据，输出工单总量 / 类目占比 / 维修完成率 / 月度报修趋势 / 维修人员工作量排名。
 * 仅系统管理员可访问（BR-7-07），故 roles 只含 admin。
 * 该页为新增页面，未触碰任何既有路由条目；原有的「统计概览看板」（Dashboard）保持逐字未变。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '居民注册', public: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { title: '找回密码', public: true }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Home',
        // 根路径按当前角色重定向到对应首页（BR-8-10）
        redirect: () => {
          const store = useUserStore()
          return ROLE_HOME[store.role] || '/dashboard'
        }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '统计概览看板', icon: 'DataBoard', roles: ['admin'] }
      },
      {
        // 模块一：报修类目管理（C-01 ~ C-07），仅系统管理员
        path: 'categories',
        name: 'CategoryManage',
        component: () => import('@/views/CategoryManage.vue'),
        meta: { title: '报修类目管理', icon: 'Files', roles: ['admin'] }
      },
      {
        // 模块三：工单审核（管理员审核工作台，D-12）
        // 承接模块二提交的「待审核（8）」工单，审核通过 → 待派单(0)，驳回 → 已驳回(9)
        path: 'orders/audit',
        name: 'OrderAudit',
        component: () => import('@/views/OrderAudit.vue'),
        meta: { title: '工单审核', icon: 'DocumentChecked', roles: ['admin'] }
      },
      {
        // 模块三：派单调度（管理员派单工作台，D-01 ~ D-05、D-10、D-11）
        // 待派单池（0）派单后流转为「已派单（1）」，流入下游维修进度管理模块
        path: 'orders/dispatch',
        name: 'OrderDispatch',
        component: () => import('@/views/OrderDispatch.vue'),
        meta: { title: '派单调度', icon: 'Promotion', roles: ['admin'] }
      },
      {
        // 模块二：我的报修（工单列表页，O-02）
        // 原为 Placeholder 占位，本阶段替换为模块二实现的真实页面
        path: 'my-orders',
        name: 'MyOrders',
        component: () => import('@/views/MyOrders.vue'),
        meta: { title: '我的报修', icon: 'Tickets', roles: ['resident'] }
      },
      {
        // 模块三：工单大厅（维修人员抢单 / 接单 / 拒单，D-06 ~ D-10）
        // 抢单池领取或管理员派单后，在本页接单确认上门；拒单则退回待派单池
        path: 'order-hall',
        name: 'GrabHall',
        component: () => import('@/views/GrabHall.vue'),
        meta: { title: '工单大厅', icon: 'Shop', roles: ['repair'] }
      },
      {
        // 模块二：提交报修 / 修改工单 / 驳回后重新提交（O-01、O-04、O-06）
        // hidden: true —— 权限仍受 roles 约束，但不作为侧边菜单项展示，
        // 入口在「我的报修」列表页的按钮中，避免菜单里出现重复入口
        path: 'orders/submit',
        name: 'OrderSubmit',
        component: () => import('@/views/OrderSubmit.vue'),
        meta: { title: '提交报修', roles: ['resident'], hidden: true }
      },
      {
        // 模块二：工单详情（O-03）。接口文档标注为「全部登录用户（受数据范围约束）」，
        // 故 roles 放开给三类角色，实际可见性由服务端的数据归属校验决定（BR-2-06）
        path: 'orders/:orderId',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '工单详情', roles: ['resident', 'repair', 'admin'], hidden: true }
      },
      {
        // 模块四：我的工单（维修进度管理模块，FR-4-01）
        // 原为 Placeholder 占位，本阶段替换为模块四实现的真实页面：
        // 列表复用模块二 O-02（服务端自动限定 worker_id = 当前用户），
        // 操作含接单（复用模块三 D-08）/ 开始维修（R-03）/ 更新进度（R-02）/
        // 提交完工（R-04）/ 进度时间轴（R-01）/ 耗时明细（R-05）
        path: 'my-tasks',
        name: 'MyTasks',
        component: () => import('@/views/MyTasks.vue'),
        meta: { title: '我的工单', icon: 'Tools', roles: ['repair'] }
      },
      {
        // 模块四：维修记录填写页（R-02 追加维修进度 + R-04 提交维修完成）
        // hidden: true —— 权限仍受 roles 约束，但不作为侧边菜单项展示，
        // 入口在「我的工单」列表页状态 3（维修中）行的「提交完工」按钮中
        path: 'my-tasks/:orderId/record',
        name: 'RepairRecordForm',
        component: () => import('@/views/RepairRecordForm.vue'),
        meta: { title: '维修记录填写', roles: ['repair'], hidden: true }
      },
      {
        // 模块五：工单验收（工单验收管理模块，V-01 ~ V-04）
        // 承接模块四 R-04 流转的「待验收(4)」工单：
        //   居民 → 查看本人提交的待验收工单、验收通过（4 → 5）/ 驳回返修（4 → 3）；
        //   管理员 → 查看全部待验收工单，对超时未验收的执行代验收（BR-5-08）。
        // 维修人员无权访问（V-01 返回 403），故 roles 不含 repair。
        path: 'order-verify',
        name: 'OrderVerify',
        component: () => import('@/views/OrderVerify.vue'),
        meta: { title: '工单验收', icon: 'CircleCheck', roles: ['resident', 'admin'] }
      },
      {
        // 模块六：服务评价（服务评价管理模块，E-01 ~ E-03）
        // 承接模块五 V-02/V-03 流转的「已验收(5)」工单：
        //   居民 → 「待评价」列表（复用模块二 O-02 + status=5）提交评价（5 → 6，业务闭环）
        //          + 「我的评价」列表（E-02，服务端按 resident_id 限定）；
        //   维修人员 → 「收到的评价」（E-02，服务端按 worker_id 限定，FR-6-07；匿名评价隐藏评价人）。
        // 管理员不用本页（另有「评价管理」页），故 roles 只含 resident 与 repair。
        path: 'my-evaluations',
        name: 'MyEvaluations',
        component: () => import('@/views/MyEvaluations.vue'),
        meta: { title: '服务评价', icon: 'Star', roles: ['resident', 'repair'] }
      },
      {
        // 模块六：评价管理（管理员评价工作台，E-02 列表 + E-04 回复）
        // 查看全部评价，支持按综合评分 / 评分上限（查低分工单）/ 类目 / 维修人员 /
        // 是否已回复 / 评价时间筛选；对未回复的评价进行一次性回复（BR-6-07）。
        path: 'evaluations',
        name: 'EvaluationManage',
        component: () => import('@/views/EvaluationManage.vue'),
        meta: { title: '评价管理', icon: 'ChatDotSquare', roles: ['admin'] }
      },
      {
        // 模块七：统计分析（报修统计分析模块，S-01 ~ S-05）
        // 数据汇总模块，上游承接模块二 ~ 模块六的全链路业务数据：
        //   S-01 概览看板（工单总量 / 维修完成率 / 平均评分）
        //   S-02 报修趋势（折线图，默认按月）
        //   S-03 类目占比（饼图 / 环形图）
        //   S-04 状态分布（0~9 恒 10 项）
        //   S-05 维修人员工作量排名（柱状图）
        // 仅系统管理员可访问（BR-7-07）；统计一律按工单提交时间过滤（BR-7-01），
        // 单次查询跨度上限 730 天（BR-7-05）。
        path: 'statistics',
        name: 'StatisticsBoard',
        component: () => import('@/views/StatisticsBoard.vue'),
        meta: { title: '统计分析', icon: 'TrendCharts', roles: ['admin'] }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', roles: ['admin'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'User', roles: ['resident', 'repair', 'admin'] }
      }
    ]
  },
  {
    // 兜底：未匹配的路径统一回到根路径（再由根路径按角色重定向）
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫：
 *  1. 公开页免登录；已登录用户访问登录/注册页时直接跳回角色首页；
 *  2. 未登录一律跳登录页，并记录来源路径便于登录后回跳；
 *  3. 已登录但刷新后 userInfo 丢失时，自动调用 U-04 恢复；
 *  4. 角色不匹配时拦截并提示。
 */
router.beforeEach(async (to) => {
  const store = useUserStore()

  // ① 公开页面
  if (to.meta.public) {
    if (store.isLogin && (to.path === '/login' || to.path === '/register')) {
      return ROLE_HOME[store.role] || '/dashboard'
    }
    return true
  }

  // ② 未登录
  if (!store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // ③ 刷新后用户信息为空，尝试用 token 重新拉取（U-04）
  if (!store.userInfo) {
    try {
      await store.fetchCurrentUser()
    } catch (e) {
      store.clear()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  // ④ 角色校验
  const allowRoles = to.meta.roles
  if (allowRoles && allowRoles.length > 0 && !allowRoles.includes(store.role)) {
    ElMessage.error('无访问权限')
    return ROLE_HOME[store.role] || '/dashboard'
  }

  return true
})

// 统一维护页面标题
router.afterEach((to) => {
  const base = '社区便民报修服务管理系统'
  document.title = to.meta.title ? `${to.meta.title} - ${base}` : base
})

export default router
