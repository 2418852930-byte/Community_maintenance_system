package com.community.repair.constant;

/**
 * 工单验收管理模块业务常量（模块五）。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）、
 * {@link OrderConstants}（模块二）、{@link DispatchConstants}（模块三）、
 * {@link ProgressConstants}（模块四）保持<b>完全一致的组织方式</b>：
 * 把长度限制、取值域与统一提示语集中定义，便于论文说明与后续维护。
 * <b>本类不修改、不覆盖前五个常量类的任何内容。</b></p>
 *
 * <p>对应需求：FR-5-01 ~ FR-5-07、BR-5-01 ~ BR-5-08；
 * 提示语逐字对齐《03-接口设计文档（最终版）》4.6 节各接口的「状态码」说明。</p>
 *
 * <p><b>为何不在此重复定义工单状态常量？</b>工单状态是全局 10 态字典，
 * 模块二的 {@link OrderConstants} 已完整定义 {@code STATUS_*} 常量，本模块直接引用，
 * 避免出现「两份状态字典」的口径分裂风险（与模块三、模块四的做法一致）。</p>
 */
public final class VerifyConstants {

    private VerifyConstants() {
    }

    // ==================================================================================
    // 一、V-02 验收通过/驳回的字段约束（接口文档 4.6.2）
    // ==================================================================================

    /**
     * 验收意见（驳回原因）长度下限。
     *
     * <p>依据接口文档 4.6.2 入参说明：{@code verifyResult = 0}（驳回）时
     * {@code verifyRemark} <b>必填，5~200 字</b>（BR-5-03）；
     * 通过时可填可不填，填写时同样受本区间约束。</p>
     */
    public static final int VERIFY_REMARK_MIN_LENGTH = 5;

    /** 验收意见长度上限（业务收紧为 200，对应 {@code repair_verify_log.verify_remark} VARCHAR(500)） */
    public static final int VERIFY_REMARK_MAX_LENGTH = 200;

    // ==================================================================================
    // 二、V-03 管理员代验收的字段约束（接口文档 4.6.3，BR-5-07）
    // ==================================================================================

    /**
     * 代验收说明长度下限。
     *
     * <p>依据 BR-5-07：管理员代验收<b>必须填写说明</b>，且同为 5~200 字。
     * 该说明落库到 {@code repair_verify_log.verify_remark}，
     * 并由 {@code verify_type = 2} 标识「这是代验收而非居民本人验收」。</p>
     */
    public static final int AGENT_REMARK_MIN_LENGTH = 5;

    /** 代验收说明长度上限（与验收意见同口径） */
    public static final int AGENT_REMARK_MAX_LENGTH = 200;

    // ==================================================================================
    // 三、取值域（「禁止跳级」与「结果/类型唯一口径」的常量集中定义）
    // ==================================================================================

    /**
     * V-02 / V-03 <b>允许验收的工单状态集合</b>：仅「待验收(4)」。
     *
     * <p>依据接口文档 4.6.2 的 409 说明——「工单状态不为 4，提示『该工单当前状态不可验收』」
     * （BR-5-01）。用数组而非单值，是为了在 Service 中以统一写法
     * （{@code in(...)} + 受影响行数校验）承载状态前置断言，
     * 后续若需扩展（例如允许对已验收工单补充说明）只需改这一处。</p>
     */
    public static final int[] VERIFIABLE_STATUSES = {OrderConstants.STATUS_PENDING_VERIFY};

    /** 验收通过的目标状态：5 已验收（解锁评价入口，FR-5-03） */
    public static final int TARGET_STATUS_PASSED = OrderConstants.STATUS_VERIFIED;

    /** 验收驳回的目标状态：3 维修中（返修，重新流入模块四，FR-5-04） */
    public static final int TARGET_STATUS_REJECTED = OrderConstants.STATUS_REPAIRING;

    /** 验收轮次起始值（{@code repair_verify_log.verify_round} 自 1 起递增，BR-5-05） */
    public static final int VERIFY_ROUND_INIT = 1;

    /**
     * 待验收超时阈值（小时）：沿用模块二 {@link OrderConstants#VERIFY_OVERDUE_HOURS} 的既定 48 小时口径。
     *
     * <p>本模块<b>不重复定义</b>该数字，只做别名引用——保证
     * 「V-01 的 {@code verifyOverdue} 标记」与「V-03 的『未达超时阈值』拦截」
     * 永远基于<strong>同一个阈值</strong>，不会出现两处各写 48 导致口径分裂。</p>
     */
    public static final int OVERDUE_HOURS = OrderConstants.VERIFY_OVERDUE_HOURS;

    /** 分页每页条数上限（接口文档 2.5 节：最大 100，与 OrderConstants 同口径） */
    public static final int MAX_PAGE_SIZE = OrderConstants.MAX_PAGE_SIZE;

    /** 分页每页条数默认值 */
    public static final int DEFAULT_PAGE_SIZE = OrderConstants.DEFAULT_PAGE_SIZE;

    /** 时间入参格式（与全局 {@code JacksonConfig} 保持一致） */
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 仅日期入参格式（V-01 的完工时间区间允许只传日期，自动补 00:00:00） */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    // ==================================================================================
    // 四、统一提示语（逐字对齐接口文档 4.6 节状态码说明）
    // ==================================================================================

    // ---------- 通用 ----------

    /** 查询成功（V-01 / V-04） */
    public static final String MSG_QUERY_SUCCESS = "查询成功";

    /** 验收通过成功（V-02，接口文档 4.6.2 响应示例的 message） */
    public static final String MSG_VERIFY_PASS_SUCCESS = "验收通过";

    /** 验收驳回成功（V-02，与通过共用接口，提示语区分以便前端展示） */
    public static final String MSG_VERIFY_REJECT_SUCCESS = "已驳回，工单返回维修中";

    /** 管理员代验收成功（V-03） */
    public static final String MSG_AGENT_VERIFY_SUCCESS = "代验收成功";

    // ---------- V-02 ----------

    /** 验收结果取值非法（400，接口文档 4.6.2：「verifyResult 越界」） */
    public static final String MSG_VERIFY_RESULT_INVALID = "验收结果只能是 0（驳回返工）或 1（通过）";

    /** 驳回未填原因（400，BR-5-03） */
    public static final String MSG_REJECT_REMARK_REQUIRED = "验收驳回时必须填写驳回原因（5~200 字）";

    /** 验收意见长度越界（400） */
    public static final String MSG_VERIFY_REMARK_INVALID = "验收意见长度须为 5~200 个字符";

    /** 状态不允许验收（409，BR-5-01） */
    public static final String MSG_STATUS_NOT_VERIFIABLE = "该工单当前状态不可验收";

    /** 重复验收（409，接口文档 4.6.2 的第二条 409） */
    public static final String MSG_ALREADY_VERIFIED = "该工单已完成验收";

    /** 非本人提交的工单（403，BR-5-06） */
    public static final String MSG_NOT_ORDER_OWNER = "无权限操作该工单，仅工单提交者可进行验收";

    // ---------- V-03 ----------

    /** 代验收说明为空或长度越界（400，BR-5-07） */
    public static final String MSG_AGENT_REMARK_INVALID = "代验收说明须为 5~200 个字符";

    /** 未达超时阈值（409，BR-5-08） */
    public static final String MSG_OVERDUE_NOT_REACHED =
            "该工单尚未超过 " + OVERDUE_HOURS + " 小时待验收超时阈值，暂不支持管理员代验收";

    /** 管理员代验收不支持驳回（400，接口文档 4.6.3：「该接口仅支持验收通过」） */
    public static final String MSG_AGENT_REJECT_UNSUPPORTED = "管理员代验收仅支持验收通过，不支持代驳回";

    // ---------- V-01 ----------

    /** V-01 完工时间区间格式非法（400） */
    public static final String MSG_TIME_FORMAT = "时间格式错误，应为 yyyy-MM-dd HH:mm:ss";

    // ---------- 通用兜底 ----------

    /** 非居民角色调用 V-02（403，兜底防御） */
    public static final String MSG_ROLE_NOT_RESIDENT = "仅居民可执行验收操作";

    /** 非管理员角色调用 V-03（403，兜底防御） */
    public static final String MSG_ROLE_NOT_ADMIN = "仅系统管理员可执行代验收操作";

    /** 无权限访问该工单（403，V-04 数据范围校验失败） */
    public static final String MSG_NO_PERMISSION_ACCESS = "无权限访问该工单";

    /** 验收记录写入冲突（409，并发下由 (order_id, verify_round) 唯一索引兜底触发） */
    public static final String MSG_VERIFY_CONFLICT = "验收操作冲突，请刷新页面后重试";
}
