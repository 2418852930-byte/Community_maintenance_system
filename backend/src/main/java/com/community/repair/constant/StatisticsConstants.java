package com.community.repair.constant;

/**
 * 报修统计分析模块业务常量（模块七）。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）、
 * {@link OrderConstants}（模块二）、{@link DispatchConstants}（模块三）、
 * {@link ProgressConstants}（模块四）、{@link VerifyConstants}（模块五）、
 * {@link EvaluationConstants}（模块六）保持<b>完全一致的组织方式</b>：
 * 把取值域、统计口径、数值精度与统一提示语集中定义，便于论文说明与后续维护。
 * <b>本类不修改、不覆盖前七个常量类的任何内容。</b></p>
 *
 * <p>对应需求：《01-需求规格说明书（最终版）》5.7 节 FR-7-01 ~ FR-7-04、FR-7-08、FR-7-10
 * 与 BR-7-01 ~ BR-7-05、BR-7-07；《03-接口设计文档（最终版）》4.8.1 ~ 4.8.5 节
 * S-01 ~ S-05 五个统计接口；《02-数据库设计与建表SQL（最终版）》方案A 附录 C（统计 SQL 示例）。
 * 提示语逐字对齐接口文档 4.8 节各接口的「状态码」说明。</p>
 *
 * <p><b>为何不在此重复定义工单状态常量？</b>工单状态是全局 10 态字典，
 * 模块二的 {@link OrderConstants} 已完整定义 {@code STATUS_*} 常量，本模块直接引用，
 * 避免出现「两份状态字典」的口径分裂风险（与模块三 ~ 模块六的做法完全一致）。</p>
 */
public final class StatisticsConstants {

    private StatisticsConstants() {
    }

    // ==================================================================================
    // 一、统计时间范围（BR-7-01 以工单提交时间为准；BR-7-05 须限定最大时间跨度）
    // ==================================================================================

    /**
     * 趋势统计的默认回溯天数（接口文档 4.8.2：{@code startTime} 不传时默认近 30 天）。
     *
     * <p>与 {@code repair_order} 的索引 {@code idx_create_time}（建表脚本注释
     * 「近30天工单趋势统计」）设计意图一致，保证默认查询即可命中索引。</p>
     */
    public static final int DEFAULT_TREND_DAYS = 30;

    /**
     * 单次查询允许的最大时间跨度（天）。
     *
     * <p>依据 BR-7-05「统计查询须限定最大时间跨度，避免全表扫描导致性能问题」，
     * 接口文档 4.8.2 明确提示语为「查询时间跨度不得超过 730 天」（约两年），
     * 超过即返回 400。</p>
     */
    public static final int MAX_RANGE_DAYS = 730;

    /**
     * 概览看板（S-01）的默认起始时间口径：当年 1 月 1 日 00:00:00。
     *
     * <p>接口文档 4.8.1：{@code startTime} 默认「当年 1 月 1 日」，
     * 故不传时由 Service 层按当前年份的首日补全，而非固定日期。</p>
     */
    public static final int OVERVIEW_DEFAULT_START_MONTH = 1;
    public static final int OVERVIEW_DEFAULT_START_DAY = 1;

    // ==================================================================================
    // 二、统计粒度（接口文档 4.8.2 granularity）
    // ==================================================================================

    /** 日粒度（默认） */
    public static final String GRANULARITY_DAY = "day";

    /** 周粒度（以周一为周期起点） */
    public static final String GRANULARITY_WEEK = "week";

    /** 月粒度 */
    public static final String GRANULARITY_MONTH = "month";

    /** 支持的粒度集合（用于入参校验，非法值返回 400） */
    public static final String[] GRANULARITIES = {
            GRANULARITY_DAY, GRANULARITY_WEEK, GRANULARITY_MONTH};

    /** 时间轴标签格式：日 → yyyy-MM-dd；周 → 周期首日 yyyy-MM-dd；月 → yyyy-MM */
    public static final String LABEL_PATTERN_DAY = "yyyy-MM-dd";
    public static final String LABEL_PATTERN_MONTH = "yyyy-MM";

    /**
     * 时间入参格式（与全局 {@code JacksonConfig.DATE_TIME_PATTERN} 保持一致）。
     *
     * <p>与 {@link EvaluationConstants#TIME_PATTERN} / {@link VerifyConstants#TIME_PATTERN}
     * 取值完全相同，本模块沿用同样的命名，保证八个模块的入参格式表述统一。</p>
     */
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 仅日期入参格式（查询条件允许只传日期，由 Service 自动补全时分秒）。
     *
     * <p>与 {@link EvaluationConstants#DATE_PATTERN} 取值一致。
     * {@code parseQueryTime} 会以 {@code value.length() == DATE_PATTERN.length()} 判定
     * 「用户只传了日期」，因此该常量同时承担「格式」与「长度哨兵」两个角色。</p>
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    // ==================================================================================
    // 三、类目聚合（接口文档 4.8.3 level / topN）
    // ==================================================================================

    /** 一级类目聚合（默认） */
    public static final int LEVEL_PRIMARY = 1;

    /** 二级类目聚合 */
    public static final int LEVEL_SECONDARY = 2;

    /** 类目层级下界（入参校验用） */
    public static final int LEVEL_MIN = LEVEL_PRIMARY;

    /** 类目层级上界（入参校验用） */
    public static final int LEVEL_MAX = LEVEL_SECONDARY;

    /** 类目分布默认返回条数（接口文档 4.8.3：topN 默认 10） */
    public static final int DEFAULT_TOP_N = 10;

    /** 类目分布最大返回条数（防止一次拉取过多类目拖慢响应，超出按上限截断） */
    public static final int MAX_TOP_N = 50;

    // ==================================================================================
    // 四、状态分组口径（单一来源 = OrderConstants，本类只做「取别名」）
    //     依据：BR-7-04 +《02-数据库设计与建表SQL》附录 C2 / C4（V2.0 已按 10 态修订）
    // ==================================================================================

    /**
     * 【有效报修量】需排除的状态：已撤销(7) 与已驳回(9)。
     *
     * <p>BR-7-04：「已撤销」工单单独成列，不计入有效报修量；「已驳回」为过程状态，
     * 同样单独成列，不计入有效报修量。附录 C4 补充：<b>待审核(8) 计入有效报修量</b>。</p>
     */
    public static final int[] VALID_EXCLUDED_STATUSES = {
            OrderConstants.STATUS_CANCELED,
            OrderConstants.STATUS_REJECTED};

    /**
     * 【完成量】对应状态：已验收(5) + 已完成(6)。
     *
     * <p>附录 C2 原文：「完成量 = 已验收(5) + 已完成(6)」。
     * 其中状态 5 为「验收通过、等待评价」，6 为「已评价、业务闭环结束」。</p>
     */
    public static final int[] FINISHED_STATUSES = {
            OrderConstants.STATUS_VERIFIED,
            OrderConstants.STATUS_FINISHED};

    /**
     * 【在途量 / 处理中】对应状态：已派单(1) + 已接单(2) + 维修中(3) + 待验收(4)。
     *
     * <p>与 {@link DispatchConstants#WORKLOAD_PENDING_STATUSES} 完全同口径，
     * 即「已进入派单链路但尚未验收」的工单。</p>
     */
    public static final int[] PROCESSING_STATUSES = {
            OrderConstants.STATUS_DISPATCHED,
            OrderConstants.STATUS_ACCEPTED,
            OrderConstants.STATUS_REPAIRING,
            OrderConstants.STATUS_PENDING_VERIFY};

    // ==================================================================================
    // 五、SQL 片段常量（编译期字符串常量，供 Mapper 的 @Select 拼接使用）
    //
    //     为什么可以直接拼进 SQL？
    //       这些片段的取值全部来自本类与 OrderConstants 中的 `public static final int`，
    //       属于【编译期常量表达式】，在编译阶段就会被替换为字面量（如 "7,9"），
    //       运行时既没有任何字符串拼接过程，也不接受任何用户输入，
    //       因此在机制上不存在 SQL 注入的可能（与模块六「类目筛选走子查询 + 预编译占位符」
    //       的防护思路一致：用户可控的值一律走 #{}，只有编译期常量才允许出现在 SQL 文本里）。
    //
    //     为什么要把它们抽成常量？
    //       保证「统计口径」全项目只有一处定义。若某天 10 态字典新增状态，
    //       只需修改 OrderConstants，本模块的分组口径会随之自动同步，不会出现
    //       「Java 常量改了、SQL 里还写着旧数字」的口径漂移。
    // ==================================================================================

    /** SQL 片段：有效报修量的排除状态列表，编译期求值为 {@code "7,9"} */
    public static final String SQL_EXCLUDED_FOR_VALID =
            OrderConstants.STATUS_CANCELED + "," + OrderConstants.STATUS_REJECTED;

    /** SQL 片段：完成量状态列表，编译期求值为 {@code "5,6"} */
    public static final String SQL_FINISHED_LIST =
            OrderConstants.STATUS_VERIFIED + "," + OrderConstants.STATUS_FINISHED;

    /** SQL 片段：在途量状态列表，编译期求值为 {@code "1,2,3,4"} */
    public static final String SQL_PROCESSING_LIST =
            OrderConstants.STATUS_DISPATCHED + "," + OrderConstants.STATUS_ACCEPTED + ","
                    + OrderConstants.STATUS_REPAIRING + "," + OrderConstants.STATUS_PENDING_VERIFY;

    /**
     * SQL 片段：状态分布的上界，编译期求值为 {@code 9}。
     *
     * <p>用于保证 S-04 一定输出 <b>10 态完整分布</b>（0~9 逐项输出，无数据的状态项也占位、
     * 其 {@code orderCount} 为 0），而不是只返回有数据的状态。
     * 取值来自 {@link OrderConstants#STATUS_MAX}，为编译期常量。</p>
     */
    public static final int STATUS_MAX = OrderConstants.STATUS_MAX;

    /**
     * SQL 片段：状态分布的下界，编译期求值为 {@code 0}。
     * 取值来自 {@link OrderConstants#STATUS_MIN}，为编译期常量。
     */
    public static final int STATUS_MIN = OrderConstants.STATUS_MIN;

    // ==================================================================================
    // 六、维修记录类型（repair_record.record_type）
    // ==================================================================================

    /**
     * 记录类型 2：到达现场 —— 维修时长的起算点。
     *
     * <p>BR-7-02：「维修时长 = 维修完成时间 − <b>开始维修时间</b>」，
     * 而《02-数据库设计与建表SQL》附录 C6 明确「开始维修时间」取
     * {@code repair_record} 中该工单<b>首条 record_type = 2（到达现场）记录的 record_time</b>。</p>
     *
     * <p>说明：模块三的 {@link DispatchConstants} 已定义 {@code RECORD_TYPE_ACCEPT = 1} 与
     * {@code RECORD_TYPE_TRANSFER_BACK = 6}，模块四的 {@link ProgressConstants} 以数组形式
     * 定义了 {@code APPENDABLE_RECORD_TYPES = {2, 3, 4}}，均未给「到达现场」单独命名。
     * 为避免修改前序模块的常量类（「能新增就不修改」原则），本模块按建表脚本注释的取值
     * 自行命名，并在注释中标明来源。</p>
     */
    public static final int RECORD_TYPE_ARRIVE = 2;

    // ==================================================================================
    // 七、数值精度（逐条对应接口文档 4.8 节的「保留 N 位小数」要求）
    // ==================================================================================

    /** 平均评分精度：保留 1 位小数（接口文档 4.8.1：avgScore 保留 1 位小数） */
    public static final int SCORE_DECIMALS = 1;

    /** 占比精度：保留 2 位小数，单位为 %（接口文档 4.8.3 / 4.8.4） */
    public static final int RATIO_DECIMALS = 2;

    /** 比率精度：保留 2 位小数，为比例（非百分数）（接口文档 4.8.1：evaluatedRate） */
    public static final int RATE_DECIMALS = 2;

    /** 占比基数 100：占比（%）= 分子 ÷ 分母 × 100，本常量集中定义避免魔法数字散落各处 */
    public static final java.math.BigDecimal PERCENT_BASE = java.math.BigDecimal.valueOf(100);

    // ==================================================================================
    // 八、统一提示语（逐字对齐接口文档 4.8 节状态码说明）
    // ==================================================================================

    /** 查询成功（S-01 ~ S-05 通用，与模块三 ~ 模块六同口径） */
    public static final String MSG_QUERY_SUCCESS = "查询成功";

    /** 统计粒度取值非法（400，接口文档 4.8.2） */
    public static final String MSG_GRANULARITY_INVALID = "统计粒度取值非法，只能是 day（日）/ week（周）/ month（月）";

    /** 时间跨度超限（400，BR-7-05，提示语逐字对齐接口文档 4.8.2） */
    public static final String MSG_RANGE_TOO_LONG = "查询时间跨度不得超过 730 天";

    /** 开始时间晚于结束时间（400） */
    public static final String MSG_TIME_ORDER_INVALID = "开始时间不能晚于结束时间";

    /** 时间格式非法（400，复用模块二口径，保证全项目提示语一致） */
    public static final String MSG_TIME_FORMAT = OrderConstants.MSG_TIME_FORMAT;

    /** 类目聚合层级取值非法（400，接口文档 4.8.3） */
    public static final String MSG_LEVEL_INVALID = "类目聚合层级取值只能是 1（一级类目）或 2（二级类目）";

    /** topN 取值非法（400，接口文档 4.8.3） */
    public static final String MSG_TOP_N_INVALID = "topN 取值范围为 1~50";

    /** 紧急程度取值非法（400，与模块二 2 级口径一致：0 普通 / 1 紧急） */
    public static final String MSG_URGENCY_INVALID = OrderConstants.MSG_URGENCY_INVALID;

    /** 非管理员角色调用统计接口（403 兜底防御，BR-7-07） */
    public static final String MSG_ROLE_NOT_ADMIN = "仅系统管理员可查看统计数据";
}
