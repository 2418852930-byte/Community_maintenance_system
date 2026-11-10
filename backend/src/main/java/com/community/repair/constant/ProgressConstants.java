package com.community.repair.constant;

/**
 * 维修进度管理模块业务常量（模块四）。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）、
 * {@link OrderConstants}（模块二）、{@link DispatchConstants}（模块三）保持完全一致的
 * 组织方式：把长度限制、数量上限、取值域与统一提示语集中定义，便于论文说明与后续维护。
 * <b>本类不修改、不覆盖前四个常量类的任何内容。</b></p>
 *
 * <p>对应需求：FR-4-01 ~ FR-4-08、BR-4-01 ~ BR-4-06；
 * 提示语逐字对齐《03-接口设计文档（最终版）》4.5 节各接口的「状态码」说明。</p>
 *
 * <p><b>为何不在此重复定义工单状态常量？</b>工单状态是全局 10 态字典，
 * 模块二的 {@link OrderConstants} 已完整定义 {@code STATUS_*} 常量，本模块直接引用，
 * 避免出现「两份状态字典」的口径分裂风险（与模块三的做法一致）。</p>
 */
public final class ProgressConstants {

    private ProgressConstants() {
    }

    // ==================================================================================
    // 一、R-02 追加维修进度的字段约束（接口文档 4.5.2）
    // ==================================================================================

    /** 进度说明长度下限（FR-4-04：2~200 字） */
    public static final int PROGRESS_CONTENT_MIN_LENGTH = 2;

    /** 进度说明长度上限（接口文档 4.5.2：2~200 字；数据库列宽 VARCHAR(500)） */
    public static final int PROGRESS_CONTENT_MAX_LENGTH = 200;

    /** 维修过程照片张数上限（FR-4-05：最多 3 张） */
    public static final int IMAGES_MAX_COUNT = 3;

    /** 图片路径总长度上限（对应字段 repair_record.images VARCHAR(1000)） */
    public static final int IMAGES_MAX_LENGTH = 1000;

    /** 配件说明长度上限（对应字段 repair_record.parts_used VARCHAR(255)） */
    public static final int PARTS_USED_MAX_LENGTH = 255;

    /** 本次费用上限（DECIMAL(10,2) 的整数位上限，防止溢出报错） */
    public static final java.math.BigDecimal COST_MAX = new java.math.BigDecimal("99999999.99");

    // ==================================================================================
    // 二、R-03 开始维修的字段约束（接口文档 4.5.3）
    // ==================================================================================

    /** 到场说明长度上限（接口文档 4.5.3：≤ 200 字） */
    public static final int ARRIVE_REMARK_MAX_LENGTH = 200;

    /** 到达现场记录的默认进度文本（未填写到场说明时使用） */
    public static final String DEFAULT_ARRIVE_CONTENT = "维修人员已到达现场，开始维修";

    // ==================================================================================
    // 三、R-04 提交维修完成的字段约束（接口文档 4.5.4）
    // ==================================================================================

    /** 维修结果说明长度下限（BR-4-04：必填，10~300 字） */
    public static final int FINISH_CONTENT_MIN_LENGTH = 10;

    /** 维修结果说明长度上限（BR-4-04：10~300 字；数据库列宽 VARCHAR(500)） */
    public static final int FINISH_CONTENT_MAX_LENGTH = 300;

    // ==================================================================================
    // 四、状态与类型取值域（「禁止跳级」的常量集中定义）
    // ==================================================================================

    /**
     * R-02 允许追加进度的工单状态集合：已接单(2) / 维修中(3)。
     *
     * <p>依据接口文档 4.5.2 的 409 说明——「工单状态不为 2 或 3（含待审核 8、已驳回 9），
     * 提示『工单已提交验收，无法继续更新进度』」。其中状态 2 追加进度不改变状态，
     * 状态 3 是最常见的追加场景；状态 4（待验收）/ 5（已验收）之后一律拒绝（BR-4-03）。</p>
     */
    public static final int[] APPENDABLE_STATUSES = {
            OrderConstants.STATUS_ACCEPTED,
            OrderConstants.STATUS_REPAIRING};

    /**
     * R-02 允许写入的记录类型：2 到达现场 / 3 维修进度 / 4 更换配件。
     *
     * <p>与 {@code RecordTypeEnum.isAppendable} 保持同一口径，此处以数组形式提供，
     * 便于在 {@code wrapper.in(...)} 中直接使用。</p>
     */
    public static final int[] APPENDABLE_RECORD_TYPES = {2, 3, 4};

    /** R-01 记录类型筛选参数的取值域：1~6（数据库 repair_record.record_type 全部取值） */
    public static final int RECORD_TYPE_MIN = 1;

    /** R-01 记录类型筛选参数的取值域上限 */
    public static final int RECORD_TYPE_MAX = 6;

    // ==================================================================================
    // 五、R-05 耗时统计口径（BR-7-02，接口文档 4.5.5）
    // ==================================================================================

    /**
     * 「全流程时长」的终点取值优先级说明。
     *
     * <p>接口文档 4.5.5 的响应示例中 {@code totalDurationMinutes = 588}，
     * 对应 {@code createTime = 09:12:30} → {@code verifyTime = 19:00:00}
     * （相隔 587.5 分钟，四舍五入为 588）。因此本模块将「全流程时长」定义为
     * <b>终点时间 − 提交时间</b>，终点时间按 {@code 验收时间 > 完工时间} 的优先级取值，
     * 二者皆为空时该指标为 {@code null}。</p>
     */
    public static final String DURATION_TOTAL_RULE = "全流程时长 = 终点时间（验收时间优先，其次完工时间）− 提交时间";

    // ==================================================================================
    // 六、统一提示语（逐字对齐接口文档 4.5 节状态码说明）
    // ==================================================================================

    // ---------- 通用 ----------

    /** 查询成功（R-01 / R-05） */
    public static final String MSG_QUERY_SUCCESS = "查询成功";

    /** 追加进度成功（R-02，接口文档 4.5.2 的 200 说明为「更新成功」） */
    public static final String MSG_RECORD_ADD_SUCCESS = "更新成功";

    /** 开始维修成功（R-03，接口文档 4.5.3 的 200 说明为「操作成功」） */
    public static final String MSG_START_SUCCESS = "操作成功";

    /** 提交维修完成成功（R-04，接口文档 4.5.4 的 200 说明为「提交成功」） */
    public static final String MSG_FINISH_SUCCESS = "提交成功";

    // ---------- R-02 ----------

    /** 记录类型取值非法（400） */
    public static final String MSG_RECORD_TYPE_INVALID =
            "记录类型只能是 2（到达现场）、3（维修进度）、4（更换配件）";

    /** 进度说明为空或长度越界（400，FR-4-04） */
    public static final String MSG_PROGRESS_CONTENT_INVALID = "进度说明长度须为 2~200 个字符";

    /** 维修过程照片超出张数上限（400，FR-4-05） */
    public static final String MSG_IMAGES_TOO_MANY = "维修过程照片最多上传 3 张";

    /** 配件说明长度越界（400） */
    public static final String MSG_PARTS_USED_TOO_LONG = "配件说明长度不能超过 255 个字符";

    /** 本次费用为负或超出列宽（400） */
    public static final String MSG_COST_INVALID = "本次费用不能为负数，且不能超过 99999999.99";

    /** 状态不允许追加进度（409，BR-4-02 / BR-4-03） */
    public static final String MSG_STATUS_NOT_APPENDABLE = "工单已提交验收，无法继续更新进度";

    // ---------- R-03 ----------

    /** 状态不允许开始维修（409，BR-4-02，禁止跳级） */
    public static final String MSG_STATUS_NOT_STARTABLE = "请按顺序更新进度";

    /** 到场说明长度越界（400） */
    public static final String MSG_ARRIVE_REMARK_TOO_LONG = "到场说明长度不能超过 200 个字符";

    // ---------- R-04 ----------

    /** 维修结果说明为空或长度越界（400，BR-4-04） */
    public static final String MSG_FINISH_CONTENT_INVALID = "维修结果说明长度须为 10~300 个字符";

    /** 状态不允许提交维修完成（409，BR-4-02，禁止跳级） */
    public static final String MSG_STATUS_NOT_FINISHABLE = "当前状态不允许提交维修完成，请按顺序更新进度";

    // ---------- R-01 ----------

    /** 记录类型筛选参数取值非法（400） */
    public static final String MSG_RECORD_TYPE_FILTER_INVALID = "记录类型筛选取值只能是 1~6";

    // ---------- 通用兜底 ----------

    /** 非维修人员角色调用进度更新类接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_REPAIR = "仅维修人员可执行该操作";

    /** 无权限访问该工单的进度数据（403，R-05 维修人员查询非本人受派工单） */
    public static final String MSG_NO_PERMISSION_ACCESS = "无权限访问该工单";
}
