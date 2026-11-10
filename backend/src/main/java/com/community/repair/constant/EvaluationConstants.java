package com.community.repair.constant;

/**
 * 服务评价管理模块业务常量（模块六）。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）、
 * {@link OrderConstants}（模块二）、{@link DispatchConstants}（模块三）、
 * {@link ProgressConstants}（模块四）、{@link VerifyConstants}（模块五）保持
 * <b>完全一致的组织方式</b>：把取值域、长度限制与统一提示语集中定义，
 * 便于论文说明与后续维护。<b>本类不修改、不覆盖前六个常量类的任何内容。</b></p>
 *
 * <p>对应需求：FR-6-01 ~ FR-6-08、BR-6-01 ~ BR-6-07；
 * 提示语逐字对齐《03-接口设计文档（最终版）》4.7 节各接口的「状态码」说明。</p>
 *
 * <p><b>为何不在此重复定义工单状态常量？</b>工单状态是全局 10 态字典，
 * 模块二的 {@link OrderConstants} 已完整定义 {@code STATUS_*} 常量，本模块直接引用，
 * 避免出现「两份状态字典」的口径分裂风险（与模块三、四、五的做法一致）。</p>
 */
public final class EvaluationConstants {

    private EvaluationConstants() {
    }

    // ==================================================================================
    // 一、星级评分（repair_evaluation.score_service / score_speed / score_quality / total_score）
    //     BR-6-03：星级评分必须为 1~5 的整数，不允许为空或越界
    // ==================================================================================

    /** 评分下限（1 星） */
    public static final int SCORE_MIN = 1;

    /** 评分上限（5 星） */
    public static final int SCORE_MAX = 5;

    /** 评分维度数量：服务态度 / 响应速度 / 维修质量，综合评分为三项均值四舍五入（接口文档 4.7 节开头） */
    public static final int SCORE_DIMENSION_COUNT = 3;

    // ==================================================================================
    // 二、评价内容与图片（repair_evaluation.content / images）
    // ==================================================================================

    /** 评价内容长度下限：0 表示内容为选填（需求文档 5.6.4「评价内容 否 0~200 字」） */
    public static final int CONTENT_MIN_LENGTH = 0;

    /** 评价内容长度上限（接口文档 4.7.1：0~200 字；数据库字段 content VARCHAR(500) 为上限冗余） */
    public static final int CONTENT_MAX_LENGTH = 200;

    /** 评价图片张数上限（接口文档 4.7.1：最多 3 张，与模块二现场照片同口径） */
    public static final int IMAGES_MAX_COUNT = 3;

    /** 评价图片路径总长度上限（对应字段 images VARCHAR(1000)） */
    public static final int IMAGES_MAX_LENGTH = 1000;

    /** 图片路径分隔符（多张以英文逗号分隔，与 repair_order.images 一致） */
    public static final String IMAGES_SEPARATOR = ",";

    // ==================================================================================
    // 三、匿名（repair_evaluation.is_anonymous）
    //     BR-6-06：维修人员只能看到与本人相关的评价，看不到评价人隐私信息
    // ==================================================================================

    /** 实名（默认值） */
    public static final int ANONYMOUS_NO = 0;

    /** 匿名 */
    public static final int ANONYMOUS_YES = 1;

    /**
     * 匿名评价对「非本人」展示的评价人姓名。
     *
     * <p>依据接口文档 4.7.2：匿名评价的 {@code residentName} 返回「匿名用户」，
     * 同时 {@code residentId} 返回 {@code null}（对非本人隐藏评价人信息）。
     * 评价人本人回看自己的评价时仍展示真实姓名，否则会出现「自己都认不出自己的评价」。</p>
     */
    public static final String ANONYMOUS_DISPLAY_NAME = "匿名用户";

    // ==================================================================================
    // 四、管理员回复（repair_evaluation.reply_content / reply_time）
    //     BR-6-07：管理员回复一条评价只能回复一次，回复后不可修改
    // ==================================================================================

    /** 回复内容长度下限（接口文档 4.7.4：回复内容 5~200 字） */
    public static final int REPLY_MIN_LENGTH = 5;

    /** 回复内容长度上限（业务收紧为 200，对应字段 reply_content VARCHAR(500)） */
    public static final int REPLY_MAX_LENGTH = 200;

    // ==================================================================================
    // 五、取值域（评价入口状态与业务闭环目标状态）
    // ==================================================================================

    /**
     * 允许提交评价的工单状态集合：仅「已验收(5)」。
     *
     * <p>依据 BR-6-01（数据库设计 3.5 节「枚举口径」）：评价入口的前置状态为工单
     * {@code status = 5 已验收}（G4 验收闭环）。用数组而非单值，是为了在 Service 中
     * 以统一写法承载状态前置断言，后续如需扩展只改这一处。</p>
     */
    public static final int[] EVALUATABLE_STATUSES = {OrderConstants.STATUS_VERIFIED};

    /**
     * 评价提交后的目标状态：6 已完成（BR-6-04）。
     *
     * <p>该状态是工单的<b>终态</b>之一：「评价提交后工单状态置为『已完成』，
     * 标志业务闭环结束」。因此「已完成(6)」在语义上等于「已评价」，
     * 这与 {@code repair_order} 上「5 已验收 = 等待评价 / 6 已完成 = 已评价」的
     * 状态机划分完全一致。</p>
     */
    public static final int TARGET_STATUS_FINISHED = OrderConstants.STATUS_FINISHED;

    /** 分页每页条数上限（接口文档 2.5 节：最大 100，与 OrderConstants 同口径） */
    public static final int MAX_PAGE_SIZE = OrderConstants.MAX_PAGE_SIZE;

    /** 分页每页条数默认值 */
    public static final int DEFAULT_PAGE_SIZE = OrderConstants.DEFAULT_PAGE_SIZE;

    /** 时间入参格式（与全局 {@code JacksonConfig} 保持一致） */
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 仅日期入参格式（查询条件允许只传日期，自动补全时分秒） */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    // ==================================================================================
    // 六、统一提示语（逐字对齐接口文档 4.7 节状态码说明）
    // ==================================================================================

    // ---------- 通用 ----------

    /** 查询成功（E-02 / E-03） */
    public static final String MSG_QUERY_SUCCESS = "查询成功";

    /** 评价提交成功（E-01，接口文档 4.7.1 响应示例的 message） */
    public static final String MSG_CREATE_SUCCESS = "评价提交成功";

    /** 回复成功（E-04） */
    public static final String MSG_REPLY_SUCCESS = "回复成功";

    // ---------- E-01 提交服务评价 ----------

    /** 工单不存在（404，接口文档 4.7.1 状态码表） */
    public static final String MSG_ORDER_NOT_FOUND = OrderConstants.MSG_ORDER_NOT_FOUND;

    /** 非本人提交的工单（403，BR-6-06 前置校验：「评价他人工单」） */
    public static final String MSG_NOT_ORDER_OWNER = "无权限操作该工单，仅工单报修人可提交评价";

    /** 工单尚未验收（409，BR-6-01，接口文档 4.7.1 提示语逐字对齐） */
    public static final String MSG_ORDER_NOT_VERIFIED = "工单尚未验收，无法评价";

    /** 重复评价（409，BR-6-02，接口文档 4.7.1 提示语逐字对齐） */
    public static final String MSG_ALREADY_EVALUATED = "该工单已评价，请勿重复提交";

    /** 评分缺失或越界（400，BR-6-03） */
    public static final String MSG_SCORE_INVALID = "星级评分必须为 1~5 的整数";

    /** 指定维度的评分越界（400，拼在维度名之后，便于前端定位是哪个维度越界） */
    public static final String MSG_SCORE_INVALID_SUFFIX = "评分必须为 1~5 的整数";

    /** 评价内容超长（400，接口文档 4.7.1） */
    public static final String MSG_CONTENT_TOO_LONG = "评价内容不能超过 200 个字符";

    /** 评价图片超出张数上限（400，接口文档 4.7.1：最多 3 张） */
    public static final String MSG_IMAGES_TOO_MANY = "评价图片最多上传 3 张";

    /** 评价图片路径总长超限（400，对应字段 images VARCHAR(1000)） */
    public static final String MSG_IMAGES_TOO_LENGTH = "评价图片路径总长度不能超过 1000 个字符";

    /** 匿名标识取值非法（400，仅允许 0 / 1） */
    public static final String MSG_ANONYMOUS_INVALID = "是否匿名取值只能是 0（实名）或 1（匿名）";

    /** 工单缺少维修人员信息（409，理论上不会出现：状态 5 的工单必有受派维修人员） */
    public static final String MSG_WORKER_MISSING = "该工单缺少维修人员信息，无法评价";

    /** 非居民角色调用提交接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_RESIDENT = "仅居民可提交服务评价";

    // ---------- E-02 评价分页列表 ----------

    /** 评分筛选值越界（400，接口文档 4.7.2） */
    public static final String MSG_SCORE_FILTER_INVALID = "评分筛选值必须为 1~5 的整数";

    /** 非管理员越权按维修人员筛选（403，接口文档 4.7.2：仅管理员可用） */
    public static final String MSG_WORKER_FILTER_FORBIDDEN = "仅管理员可按维修人员筛选评价";

    /** 非管理员越权按类目筛选（403，接口文档 4.7.2：仅管理员可用） */
    public static final String MSG_CATEGORY_FILTER_FORBIDDEN = "仅管理员可按类目筛选评价";

    /** 时间格式非法（400） */
    public static final String MSG_TIME_FORMAT = "时间格式错误，应为 yyyy-MM-dd HH:mm:ss";

    // ---------- E-03 评价详情 ----------

    /** 评价不存在（404，接口文档 4.7.3） */
    public static final String MSG_EVALUATION_NOT_FOUND = "评价不存在";

    /** 无权限访问该评价（403，接口文档 4.7.3：数据归属校验不通过） */
    public static final String MSG_NO_PERMISSION_VIEW = "无权限访问该评价";

    // ---------- E-04 回复评价 ----------

    /** 回复内容为空或长度越界（400） */
    public static final String MSG_REPLY_INVALID = "回复内容须为 5~200 个字符";

    /** 该评价已回复（409，BR-6-07，接口文档 4.7.4 提示语逐字对齐） */
    public static final String MSG_ALREADY_REPLIED = "该评价已回复，不可重复回复";

    /** 非管理员角色调用回复接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_ADMIN = "仅系统管理员可回复评价";
}
