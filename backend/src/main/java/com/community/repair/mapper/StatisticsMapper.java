package com.community.repair.mapper;

import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.StatisticsConstants;
import com.community.repair.vo.CategoryStatItemVO;
import com.community.repair.vo.StatusStatItemVO;
import com.community.repair.vo.StatisticsOverviewVO;
import com.community.repair.vo.TrendItemVO;
import com.community.repair.vo.WorkerStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报修统计分析 Mapper（模块七，<b>只读</b>）。
 *
 * <p>本 Mapper 是模块七的<b>唯一数据访问出口</b>，承载 S-01 ~ S-05 五个统计接口所需的
 * 全部聚合查询。它<b>不继承</b> {@code BaseMapper}，因为统计查询只需「读 + 聚合」，
 * 不需要任何实体 CRUD 能力（与模块三的 {@code RepairOrderStatMapper} 采用同一策略）。</p>
 *
 * <h3>一、为什么单独建一个 Mapper，而不往前序模块的 Mapper 里加方法？</h3>
 * <ol>
 *   <li><b>不修改前序模块</b>：{@code RepairOrderMapper} / {@code RepairEvaluationMapper} /
 *       {@code RepairCategoryMapper} 分别属于模块二 / 模块六 / 模块一的交付边界，
 *       本模块遵循「能新增就不修改」原则，一个字节都不动它们；</li>
 *   <li><b>读写分离语义清晰</b>：本 Mapper 只承载 {@code GROUP BY} 与 {@code SUM(CASE WHEN)}
 *       这类聚合查询，与实体读写型 Mapper 的职责分离，便于论文论述；</li>
 *   <li><b>统计口径集中</b>：全部统计 SQL 集中在一个文件里，口径变更只需改一处。</li>
 * </ol>
 *
 * <h3>二、五个共性设计要点</h3>
 *
 * <p><b>要点 1 —— 状态口径不在 SQL 里硬编码，而是引用编译期常量。</b>
 * SQL 文本中出现 {@code status IN (5,6)}、{@code status NOT IN (7,9)} 这类写法时，
 * 数字来自 {@link StatisticsConstants} 的 {@code SQL_*} 常量与 {@link OrderConstants} 的
 * {@code STATUS_*} 常量，它们在<b>编译期</b>就被替换为字面量。
 * 这样「10 态字典」全项目仍然只有一处定义，不会出现「Java 常量改了、SQL 里还写着旧数字」
 * 的口径漂移；同时它不是运行期拼接、更不接受任何用户输入，
 * 机制上不存在 SQL 注入面。</p>
 *
 * <p><b>要点 2 —— 用户可控的值一律走 {@code #{}} 预编译占位符。</b>
 * 时间范围、类目 ID、维修人员 ID、紧急程度、topN 全部使用 {@code #{...}}，
 * 由 JDBC {@code PreparedStatement} 绑定参数。</p>
 *
 * <p><b>要点 3 —— 增量计数用 {@code SUM(CASE WHEN ...)}，一次往返算出多个指标。</b>
 * 概览看板需要 10 个状态计数，若写 10 条 SQL 会造成 10 次数据库往返；
 * 这里用一条 SQL 的多个 {@code SUM(CASE ...)} 表达式一次性算出，
 * 走 {@code idx_status_create(status, create_time)} / {@code idx_create_time(create_time)} 索引。</p>
 *
 * <p><b>要点 4 —— 时间过滤一律作用于 {@code repair_order.create_time}。</b>
 * 这是 BR-7-01 的核心口径：「统计口径以工单的提交时间为准进行时间范围过滤」。
 * 唯一的例外是评价相关聚合（{@code selectAvgScore} / {@code countEvaluatedOrders}
 * / {@code selectWorkerAvgScore}），它们作用于 {@code repair_evaluation.create_time}（评价时间），
 * 因为评价表上不存在工单提交时间——这也是接口文档 4.8.7 对评价类筛选的既定口径。</p>
 *
 * <p><b>要点 5 —— 类目筛选统一采用「分支语义」。</b>
 * 本项目的工单可直接挂在一级类目上，也可挂在二级子类目上，因此
 * {@code categoryId} 筛选统一展开为
 * {@code category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0
 * AND (category_id = #{categoryId} OR parent_id = #{categoryId}))}，
 * 即「选中父类目时连同其全部子类目一并纳入，选中子类目时精确定位该子类目」。
 * 五个接口共用同一段子查询，保证 S-02 ~ S-05 的类目口径完全一致；
 * 该写法<b>不新增字段、不新增表</b>，且全部走 {@code #{}} 占位符，无注入风险。</p>
 *
 * <h3>三、SQL 与建表脚本附录 C 的对应关系</h3>
 * <table border="1">
 *   <caption>方法 ↔ 附录 C 示例 ↔ 接口</caption>
 *   <tr><th>本 Mapper 方法</th><th>附录 C 示例</th><th>接口</th></tr>
 *   <tr><td>{@link #selectOverviewCounts(Long)}</td><td>C4 工单状态分布</td><td>S-01</td></tr>
 *   <tr><td>{@link #selectTrendByDay} / {@code ByWeek} / {@code ByMonth}</td><td>C3 近 30 天趋势</td><td>S-02</td></tr>
 *   <tr><td>{@link #selectCategoryStatLevel1} / {@code Level2}</td><td>C1 各类目工单量</td><td>S-03</td></tr>
 *   <tr><td>{@link #selectStatusDistribution}</td><td>C4 工单状态分布</td><td>S-04</td></tr>
 *   <tr><td>{@link #selectWorkerWorkload}</td><td>C2 各维修人员工作量</td><td>S-05</td></tr>
 *   <tr><td>{@link #selectAvgRepairMinutes}</td><td>C6 维修时效三指标</td><td>S-01</td></tr>
 * </table>
 *
 * <h3>四、编写 {@code @Select} 的强制约定（务必遵守，防止 500 复发）</h3>
 * <p>本 Mapper 的 SQL 有两种<b>互斥</b>的合法写法，混用会直接导致接口 500：</p>
 * <table border="1">
 *   <caption>两种写法的适用场景与比较符要求</caption>
 *   <tr><th>写法</th><th>适用场景</th><th>比较符写法</th></tr>
 *   <tr><td><b>写法一</b>：加 {@code <script>} 包裹</td>
 *       <td>含 {@code <if>} / {@code <foreach>} 等动态标签</td>
 *       <td>必须写 XML 实体 {@code &gt;=} / {@code &lt;=}</td></tr>
 *   <tr><td><b>写法二</b>：不加 {@code <script>}</td>
 *       <td>纯静态 SQL（无动态标签）</td>
 *       <td>直接写 {@code >=} / {@code &lt;=}，<b>禁止</b>写实体</td></tr>
 * </table>
 *
 * <p><b>原理</b>：MyBatis 的 {@code XMLLanguageDriver} 仅当注解字符串以
 * {@code <script>} 开头时才走 XML 解析分支（此时实体被解码为比较符）；
 * 否则走 {@code RawSqlSource} 分支，<b>只处理 {@code #{}} 占位符、不解码任何 XML 实体</b>。</p>
 *
 * <p><b>反面案例（本项目真实缺陷）</b>：{@code countTodayOrders} 原先在写法二下写了
 * {@code &gt;=}，SQL 原样带着 {@code &gt;=} 发给 MySQL，
 * 报 {@code SQLSyntaxErrorException: ... near '= CURDATE()'}，
 * 使 {@code /api/statistics/overview} 返回 500。修复方式见该方法注释。</p>
 *
 * <p><b>为什么 {@code >=} 可以在写法二里直接写？</b>注解值是 Java 字符串字面量，
 * 编译期不涉及 XML，{@code >} 与 {@code <} 都无需转义；
 * 只有写法一才需要为了通过 XPath 解析而使用实体。</p>
 *
 * <p><b>自查口令</b>：遍历本文件所有 {@code @Select} —— 若某条<b>没有</b> {@code <script>} 开头，
 * 其字符串里就不允许出现 {@code &gt;} 或 {@code &lt;} 这四个字符。</p>
 */
@Mapper
public interface StatisticsMapper {

    // ==================================================================================
    // 一、S-01 统计概览看板
    // ==================================================================================

    /**
     * 概览看板的 9 项状态计数（一条 SQL、一次数据库往返算出）。
     *
     * <p>口径逐项说明：</p>
     * <ul>
     *   <li>{@code total_order_count}：{@code status NOT IN (7,9)}，即有效报修量（<b>含待审核 8</b>）；</li>
     *   <li>{@code pending_audit_count}：状态 8，管理员审核待办量；</li>
     *   <li>{@code pending_dispatch_count}：状态 0；</li>
     *   <li>{@code processing_count}：状态 1 ~ 4（在途）；</li>
     *   <li>{@code pending_verify_count}：状态 4（在途的子集）；</li>
     *   <li>{@code verified_count}：状态 5（完成量的组成部分，用于算完成率）；</li>
     *   <li>{@code completed_count}：状态 6；</li>
     *   <li>{@code cancel_count}：状态 7，单独成列；</li>
     *   <li>{@code rejected_count}：状态 9，单独成列。</li>
     * </ul>
     *
     * <p>{@code IFNULL(..., 0)} 的作用：区间内一条工单都没有时 {@code SUM} 会返回 {@code NULL}，
     * 若不包裹，前端会拿到 {@code null} 而不是 0
     * （接口文档 4.8.1 状态码表要求「无数据时数值类返回 0」）。</p>
     *
     * @param startTime 统计时间起（含）
     * @param endTime   统计时间止（含）
     * @return 计数承载对象（派生指标字段留待 Service 填充）
     */
    @Select("<script>"
            + "SELECT "
            + "IFNULL(SUM(CASE WHEN status NOT IN (" + StatisticsConstants.SQL_EXCLUDED_FOR_VALID
            + ") THEN 1 ELSE 0 END), 0) AS total_order_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_PENDING_AUDIT
            + " THEN 1 ELSE 0 END), 0) AS pending_audit_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_PENDING_DISPATCH
            + " THEN 1 ELSE 0 END), 0) AS pending_dispatch_count, "
            + "IFNULL(SUM(CASE WHEN status IN (" + StatisticsConstants.SQL_PROCESSING_LIST
            + ") THEN 1 ELSE 0 END), 0) AS processing_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_PENDING_VERIFY
            + " THEN 1 ELSE 0 END), 0) AS pending_verify_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_VERIFIED
            + " THEN 1 ELSE 0 END), 0) AS verified_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_FINISHED
            + " THEN 1 ELSE 0 END), 0) AS completed_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_CANCELED
            + " THEN 1 ELSE 0 END), 0) AS cancel_count, "
            + "IFNULL(SUM(CASE WHEN status = " + OrderConstants.STATUS_REJECTED
            + " THEN 1 ELSE 0 END), 0) AS rejected_count "
            + "FROM repair_order "
            + "WHERE deleted = 0 "
            + "AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}"
            + "</script>")
    StatisticsOverviewVO selectOverviewCounts(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 今日新增工单数（含待审核）。
     *
     * <p><b>【缺陷修复】</b>本方法原先在<b>没有</b> {@code <script>} 包裹的情况下写了 XML 实体
     * {@code &gt;=}，而 MyBatis 只对以 {@code <script>} 开头的 SQL 做 XML 解析，
     * 其余走 {@code RawSqlSource} 分支、<b>不解码实体</b>，导致 {@code &gt;=} 原样发给 MySQL，
     * 报 {@code SQLSyntaxErrorException: ... near '= CURDATE()'}，最终表现为
     * {@code /api/statistics/overview} 返回 500。
     * 现改为直接写 {@code >=}（Java 字符串中比较符无需转义）。</p>
     *
     * <p><b>编码约定</b>：静态 SQL 不加 {@code <script>} 时，比较符一律直接写 {@code >=} / {@code <=}；
     * 只有用 {@code <script>} 包裹的动态 SQL 才写 {@code &gt;=} / {@code &lt;=}。两者不可混用。</p>
     *
     * <p>口径：{@code create_time >= CURDATE()} 的未删除工单，<b>不按所选时间范围过滤</b>
     * （接口文档 4.8.1：该指标与 {@code startTime/endTime} 无关，恒为「今天」）。
     * 不额外排除任何状态：接口文档原文标注「今日新增工单数（含待审核）」，
     * 即不做状态过滤，是一个纯粹的「今日提交量」指标。</p>
     *
     * @return 今日新增工单数（无数据时为 0）
     */
    @Select("SELECT COUNT(*) FROM repair_order "
            + "WHERE deleted = 0 AND create_time >= CURDATE()")
    Long countTodayOrders();

    /**
     * 平均综合评分（保留 1 位小数）。
     *
     * <p>依据 BR-7-03「平均评分仅统计已产生评价的工单，未评价工单不计入分母」，
     * 直接对 {@code repair_evaluation.total_score} 求平均即可——
     * 表里存在的行就是「已产生评价的工单」，天然满足该规则。
     * 无任何评价时返回 {@code null}（接口文档 4.8.1：「无评价时为 null」）。</p>
     *
     * @param startTime 评价时间起（含）
     * @param endTime   评价时间止（含）
     * @return 平均评分，无评价时为 {@code null}
     */
    @Select("SELECT ROUND(AVG(total_score), " + StatisticsConstants.SCORE_DECIMALS + ") "
            + "FROM repair_evaluation "
            + "WHERE deleted = 0 "
            + "AND create_time >= #{startTime} AND create_time <= #{endTime}")
    BigDecimal selectAvgScore(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 已评价工单数（评价率 {@code evaluatedRate} 的分子）。
     *
     * @param startTime 评价时间起（含）
     * @param endTime   评价时间止（含）
     * @return 已评价工单数
     */
    @Select("SELECT COUNT(*) FROM repair_evaluation "
            + "WHERE deleted = 0 "
            + "AND create_time >= #{startTime} AND create_time <= #{endTime}")
    Long countEvaluatedOrders(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 平均接单时长（分钟，四舍五入取整）。
     *
     * <p>口径依据 BR-7-02：接单时长 = 接单时间 − 派单时间
     * （{@code accept_time − dispatch_time}）。样本仅含
     * {@code worker_id} 非空且两个时间点齐全的工单。
     * 待审核(8) / 已驳回(9) 因 {@code worker_id} 恒为空而<b>天然被排除</b>，
     * 无需额外状态条件（与接口文档 4.8.6 的样本口径说明一致）。</p>
     *
     * @param startTime 工单提交时间起（含）
     * @param endTime   工单提交时间止（含）
     * @return 平均接单分钟数，无样本时为 {@code null}
     */
    @Select("SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, dispatch_time, accept_time))) "
            + "FROM repair_order "
            + "WHERE deleted = 0 AND worker_id IS NOT NULL "
            + "AND dispatch_time IS NOT NULL AND accept_time IS NOT NULL "
            + "AND create_time >= #{startTime} AND create_time <= #{endTime}")
    Long selectAvgAcceptMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 平均维修时长（分钟，四舍五入取整）。
     *
     * <p>口径依据 BR-7-02：维修时长 = 维修完工时间 − <b>开始维修时间</b>，
     * 而「开始维修时间」按建表脚本附录 C6 取该工单在 {@code repair_record} 中
     * <b>首条「到达现场(2)」记录的 {@code record_time}</b>
     * （{@code MIN(record_time)} 即为首条，因为同一工单的到达现场记录只会写一次）。</p>
     *
     * <p>内联子查询先按 {@code order_id} 聚合出每张工单的到达时间，
     * 再与工单表 JOIN。这样做的两个好处：① 避免在 {@code AVG} 里出现相关子查询导致逐行扫描；
     * ② 走 {@code repair_record} 的 {@code idx_order_time(order_id, record_time)} 索引。</p>
     *
     * @param startTime 工单提交时间起（含）
     * @param endTime   工单提交时间止（含）
     * @return 平均维修分钟数，无样本时为 {@code null}
     */
    @Select("SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, a.arrive_time, o.finish_time))) "
            + "FROM repair_order o "
            + "JOIN (SELECT order_id, MIN(record_time) AS arrive_time FROM repair_record "
            + "      WHERE record_type = " + StatisticsConstants.RECORD_TYPE_ARRIVE
            + " AND deleted = 0 GROUP BY order_id) a ON a.order_id = o.order_id "
            + "WHERE o.deleted = 0 AND o.worker_id IS NOT NULL "
            + "AND o.finish_time IS NOT NULL "
            + "AND o.create_time >= #{startTime} AND o.create_time <= #{endTime}")
    Long selectAvgRepairMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ==================================================================================
    // 二、S-02 报修量趋势统计
    //     三个粒度各写一条 SQL（day / week / month），而不是把分组表达式做成参数拼进 SQL。
    //     原因是：分组表达式属于「SQL 结构」而非「数据」，用 ${} 拼字符串虽然在本场景下
    //     也已由白名单保护，但保留 ${} 就等于留下一个未来可能被误用的注入入口。
    //     三条静态 SQL 把这件事从机制上堵死，代价只是多写两个方法。
    // ==================================================================================

    /**
     * S-02 日粒度趋势：按 {@code yyyy-MM-dd} 分组。
     *
     * @param startTime  工单提交时间起（含）
     * @param endTime    工单提交时间止（含）
     * @param categoryId 类目筛选（可空，走分支语义）
     * @param workerId   维修人员筛选（可空）
     * @param urgency    紧急程度筛选（可空，0 普通 / 1 紧急）
     * @return 按时间升序的时间序列（仅含有数据的时间点）
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(o.create_time, '%Y-%m-%d') AS time_label, "
            + "COUNT(o.order_id) AS order_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS completed_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_CANCELED
            + " THEN 1 ELSE 0 END), 0) AS cancel_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_REJECTED
            + " THEN 1 ELSE 0 END), 0) AS rejected_count "
            + "FROM repair_order o "
            + "WHERE o.deleted = 0 "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "<if test='urgency != null'>AND o.urgency = #{urgency} </if>"
            + "GROUP BY DATE_FORMAT(o.create_time, '%Y-%m-%d') "
            + "ORDER BY time_label ASC"
            + "</script>")
    List<TrendItemVO> selectTrendByDay(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId,
            @Param("urgency") Integer urgency);

    /**
     * S-02 周粒度趋势：按「该周周一」的 {@code yyyy-MM-dd} 分组。
     *
     * <p>{@code WEEKDAY(create_time)} 返回 0（周一）~ 6（周日），
     * 用 {@code DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY)}
     * 把每一天归到它所在周的周一，从而保证「跨月的一周」不会被拆成两段，
     * 也避免了 {@code YEARWEEK()} 需要额外处理年份边界与 {@code mode} 参数的问题。</p>
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(DATE_SUB(o.create_time, INTERVAL WEEKDAY(o.create_time) DAY), "
            + "'%Y-%m-%d') AS time_label, "
            + "COUNT(o.order_id) AS order_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS completed_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_CANCELED
            + " THEN 1 ELSE 0 END), 0) AS cancel_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_REJECTED
            + " THEN 1 ELSE 0 END), 0) AS rejected_count "
            + "FROM repair_order o "
            + "WHERE o.deleted = 0 "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "<if test='urgency != null'>AND o.urgency = #{urgency} </if>"
            + "GROUP BY DATE_FORMAT(DATE_SUB(o.create_time, INTERVAL WEEKDAY(o.create_time) DAY), "
            + "'%Y-%m-%d') "
            + "ORDER BY time_label ASC"
            + "</script>")
    List<TrendItemVO> selectTrendByWeek(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId,
            @Param("urgency") Integer urgency);

    /**
     * S-02 月粒度趋势：按 {@code yyyy-MM} 分组（前端折线图的默认视图）。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(o.create_time, '%Y-%m') AS time_label, "
            + "COUNT(o.order_id) AS order_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS completed_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_CANCELED
            + " THEN 1 ELSE 0 END), 0) AS cancel_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_REJECTED
            + " THEN 1 ELSE 0 END), 0) AS rejected_count "
            + "FROM repair_order o "
            + "WHERE o.deleted = 0 "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "<if test='urgency != null'>AND o.urgency = #{urgency} </if>"
            + "GROUP BY DATE_FORMAT(o.create_time, '%Y-%m') "
            + "ORDER BY time_label ASC"
            + "</script>")
    List<TrendItemVO> selectTrendByMonth(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId,
            @Param("urgency") Integer urgency);

    // ==================================================================================
    // 三、S-03 类目分布统计
    // ==================================================================================

    /**
     * S-03 一级类目聚合：把挂在子类目上的工单归并到它的根类目。
     *
     * <p>归并表达式 {@code IF(c.parent_id = 0, c.category_id, c.parent_id)} 的含义：
     * 一级类目（{@code parent_id = 0}）取自身 ID，二级类目取其父 ID。
     * {@code GROUP BY} 与 {@code SELECT} 中重复写同一表达式，是为了在
     * 开启 {@code ONLY_FULL_GROUP_BY} 的 MySQL 5.7+ / 8.0 上同样成立。</p>
     *
     * <p>统计范围含待审核(8)、不含已撤销(7) 与已驳回(9)，对齐 BR-7-04
     * （接口文档 4.8.3 V2.0 口径说明）。</p>
     *
     * @param topN 仅返回前 N 项（1~50，Service 已做取值域保护）
     */
    @Select("<script>"
            + "SELECT IF(c.parent_id = 0, c.category_id, c.parent_id) AS category_id, "
            + "COUNT(o.order_id) AS order_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS completed_count "
            + "FROM repair_order o "
            + "JOIN repair_category c ON c.category_id = o.category_id AND c.deleted = 0 "
            + "WHERE o.deleted = 0 AND o.status NOT IN (" + StatisticsConstants.SQL_EXCLUDED_FOR_VALID + ") "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND (c.category_id = #{categoryId} OR c.parent_id = #{categoryId}) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "GROUP BY IF(c.parent_id = 0, c.category_id, c.parent_id) "
            + "ORDER BY order_count DESC "
            + "LIMIT #{topN}"
            + "</script>")
    List<CategoryStatItemVO> selectCategoryStatLevel1(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId,
            @Param("topN") Integer topN);

    /**
     * S-03 二级类目聚合：按工单实际挂载的类目（{@code c.category_id}）分组。
     */
    @Select("<script>"
            + "SELECT c.category_id AS category_id, "
            + "COUNT(o.order_id) AS order_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS completed_count "
            + "FROM repair_order o "
            + "JOIN repair_category c ON c.category_id = o.category_id AND c.deleted = 0 "
            + "WHERE o.deleted = 0 AND o.status NOT IN (" + StatisticsConstants.SQL_EXCLUDED_FOR_VALID + ") "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND (c.category_id = #{categoryId} OR c.parent_id = #{categoryId}) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "GROUP BY c.category_id "
            + "ORDER BY order_count DESC "
            + "LIMIT #{topN}"
            + "</script>")
    List<CategoryStatItemVO> selectCategoryStatLevel2(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId,
            @Param("topN") Integer topN);

    /**
     * 有效报修量（S-03 / S-04 的占比分母）。
     *
     * <p>口径：{@code status NOT IN (7,9)}，含待审核(8)。
     * 该值必须<b>独立查询</b>，不能拿前端返回的类目明细求和——
     * 因为 S-03 受 {@code topN} 截断，求和会小于真实总量，导致占比整体偏大。</p>
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM repair_order o "
            + "WHERE o.deleted = 0 AND o.status NOT IN (" + StatisticsConstants.SQL_EXCLUDED_FOR_VALID + ") "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "</script>")
    Long countValidOrders(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId);

    // ==================================================================================
    // 四、S-04 工单状态分布统计
    // ==================================================================================

    /**
     * S-04 状态分布：按 {@code status} 分组统计数量（只返回有数据的状态）。
     *
     * <p>本方法<b>只返回数据库里真实存在的状态</b>；「补齐 0~9 全部 10 个状态、
     * 无数据的项填 0」这一步放在 Service 层完成。理由：
     * ① 状态字典的唯一权威是 Java 侧 {@code OrderStatusEnum}，SQL 不该承载状态名单；
     * ② 在 Java 里 {@code for (0..9)} 循环补齐比 SQL 里写 {@code UNION} 十个 {@code SELECT} 更清晰。</p>
     *
     * <p>注意本方法<b>不过滤</b>已撤销(7) 与已驳回(9)：状态分布需要完整展示这两项
     * （接口文档 4.8.4：「已撤销」与「已驳回」工单单独成列）。</p>
     */
    @Select("<script>"
            + "SELECT o.status AS status, COUNT(o.order_id) AS order_count "
            + "FROM repair_order o "
            + "WHERE o.deleted = 0 "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "GROUP BY o.status "
            + "ORDER BY o.status ASC"
            + "</script>")
    List<StatusStatItemVO> selectStatusDistribution(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId);

    // ==================================================================================
    // 五、S-05 维修人员工作量统计
    // ==================================================================================

    /**
     * S-05 维修人员工作量聚合（接单量 / 完成量 / 在途量 / 撤销量 / 平均维修时长）。
     *
     * <p>SQL 只扫 {@code repair_order}（外加一个聚合后的到达时间派生表），
     * 用 {@code COUNT} + 四个 {@code SUM(CASE WHEN ...)} 一次算出全部指标，
     * 避免「每人一个指标一次查询」的 N×M 次往返。</p>
     *
     * <p>{@code LEFT JOIN} 派生表而不用内联子查询：派生表已按 {@code order_id} 聚合成
     * 每单至多一行，因此不会造成工单数被放大；{@code LEFT JOIN} 保证
     * 「没有到达现场记录的工单」也会被计入接单量，只是不参与维修时长均值
     * （{@code CASE WHEN ... THEN ... END} 在条件不满足时返回 {@code NULL}，
     * 而 {@code AVG} 会自动忽略 {@code NULL}，正好实现了
     * 「有则计入、无则跳过」的语义）。</p>
     *
     * <p>排序：按完成量降序，完成量相同再按接单量降序，保证排名稳定
     * （接口文档 4.8.5：「默认按完成量降序排列」）。</p>
     */
    @Select("<script>"
            + "SELECT o.worker_id AS worker_id, "
            + "COUNT(o.order_id) AS accept_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_FINISHED_LIST
            + ") THEN 1 ELSE 0 END), 0) AS finish_count, "
            + "IFNULL(SUM(CASE WHEN o.status IN (" + StatisticsConstants.SQL_PROCESSING_LIST
            + ") THEN 1 ELSE 0 END), 0) AS processing_count, "
            + "IFNULL(SUM(CASE WHEN o.status = " + OrderConstants.STATUS_CANCELED
            + " THEN 1 ELSE 0 END), 0) AS cancel_count, "
            + "ROUND(AVG(CASE WHEN a.arrive_time IS NOT NULL AND o.finish_time IS NOT NULL "
            + "THEN TIMESTAMPDIFF(MINUTE, a.arrive_time, o.finish_time) END)) AS avg_repair_minutes "
            + "FROM repair_order o "
            + "LEFT JOIN (SELECT order_id, MIN(record_time) AS arrive_time FROM repair_record "
            + "      WHERE record_type = " + StatisticsConstants.RECORD_TYPE_ARRIVE
            + " AND deleted = 0 GROUP BY order_id) a ON a.order_id = o.order_id "
            + "WHERE o.deleted = 0 AND o.worker_id IS NOT NULL "
            + "AND o.create_time &gt;= #{startTime} AND o.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND o.worker_id = #{workerId} </if>"
            + "GROUP BY o.worker_id "
            + "ORDER BY finish_count DESC, accept_count DESC"
            + "</script>")
    List<WorkerStatVO> selectWorkerWorkload(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId);

    /**
     * S-05 各维修人员的平均综合评分（保留 1 位小数），供 Service 按 {@code workerId} 合并。
     *
     * <p>单独成一条 SQL 而不是塞进 {@link #selectWorkerWorkload}：
     * 两张表的聚合粒度不同（一个按工单、一个按评价），硬 JOIN 会在
     * 人员同时有 N 张工单与 M 条评价时产生 N×M 行，从而把完成量算成 N×M。
     * 拆成两条「各自聚合、按 workerId 合并」是唯一不会放大计数的写法。</p>
     *
     * <p>评价时间范围与工单时间范围共用同一组入参（都取 {@code startTime/endTime}），
     * 其中工单侧用于类目筛选，评价侧用于时间筛选——这一点已在类注释「要点 4」中说明。</p>
     */
    @Select("<script>"
            + "SELECT e.worker_id AS worker_id, "
            + "ROUND(AVG(e.total_score), " + StatisticsConstants.SCORE_DECIMALS + ") AS avg_score "
            + "FROM repair_evaluation e "
            + "JOIN repair_order o ON o.order_id = e.order_id AND o.deleted = 0 "
            + "WHERE e.deleted = 0 "
            + "AND e.create_time &gt;= #{startTime} AND e.create_time &lt;= #{endTime} "
            + "<if test='categoryId != null'>"
            + "AND o.category_id IN (SELECT category_id FROM repair_category WHERE deleted = 0 "
            + "AND (category_id = #{categoryId} OR parent_id = #{categoryId})) "
            + "</if>"
            + "<if test='workerId != null'>AND e.worker_id = #{workerId} </if>"
            + "GROUP BY e.worker_id"
            + "</script>")
    List<WorkerStatVO> selectWorkerAvgScore(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("categoryId") Long categoryId,
            @Param("workerId") Long workerId);
}
