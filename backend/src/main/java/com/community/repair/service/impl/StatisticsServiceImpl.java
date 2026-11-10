package com.community.repair.service.impl;

import com.community.repair.common.BusinessException;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.constant.StatisticsConstants;
import com.community.repair.dto.StatisticsQueryDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.OrderStatusEnum;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.mapper.StatisticsMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.StatisticsService;
import com.community.repair.vo.CategoryStatItemVO;
import com.community.repair.vo.StatusStatItemVO;
import com.community.repair.vo.StatisticsCategoryVO;
import com.community.repair.vo.StatisticsOverviewVO;
import com.community.repair.vo.StatisticsStatusVO;
import com.community.repair.vo.StatisticsTrendVO;
import com.community.repair.vo.TrendItemVO;
import com.community.repair.vo.WorkerStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 报修统计分析模块 业务实现（模块七）。
 *
 * <p>本类集中承载需求文档 5.7 节 FR-7-01 ~ FR-7-04、FR-7-08、FR-7-10 与
 * BR-7-01 ~ BR-7-05、BR-7-07 的全部业务规则，每个方法上方的注释都标注了对应规则编号，
 * 便于论文与答辩溯源。</p>
 *
 * <h3>一、复用的基础组件（不重复创建）</h3>
 * <ul>
 *   <li>统一响应体 {@code Result} / 状态码字典 {@code ResultCode} + 全局异常处理器
 *       {@code GlobalExceptionHandler}；</li>
 *   <li>鉴权与角色校验 {@code JwtInterceptor} / {@code @RequireRole} / {@code UserContext}
 *       —— 本模块<b>不新增任何鉴权组件</b>，仅在 Service 内增加一层角色兜底断言；</li>
 *   <li>状态字典 {@code OrderStatusEnum} 与常量 {@code OrderConstants}
 *       —— <b>本模块不重复定义工单状态</b>，S-04 的 10 态中文名直接来自该枚举；</li>
 *   <li>类目字典 {@code RepairCategory} / {@code RepairCategoryMapper}（模块一）与
 *       用户档案 {@code SysUser} / {@code SysUserMapper}（模块零），本模块只读复用；</li>
 *   <li>聚合查询统一走本模块新建的 {@code StatisticsMapper}。</li>
 * </ul>
 *
 * <h3>二、四个贯穿全类的方法级设计</h3>
 *
 * <p><b>设计 1 —— 时间参数解析统一。五个接口共用同一套解析规则。</b>
 * {@link #parseQueryTime} 同时兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd}，
 * 只传日期时「起」补 {@code 00:00:00}、「止」补 {@code 23:59:59}
 * （与模块六 E-02 的做法完全一致，前端 {@code el-date-picker} 的 daterange 无需拼时分秒）。</p>
 *
 * <p><b>设计 2 —— 时间跨度统一封顶。</b>
 * 所有接口在解析出起止时间后都调用 {@link #checkRange}，
 * 跨度超过 {@link StatisticsConstants#MAX_RANGE_DAYS}（730 天）一律返回 400，
 * 落实 BR-7-05「统计查询须限定最大时间跨度，避免全表扫描导致性能问题」。</p>
 *
 * <p><b>设计 3 —— 占比 / 比率统一计算，避免每个方法各写一份除零判断。</b>
 * {@link #percentage} 输出「百分数（单位 %，2 位小数，HALF_UP）」，
 * {@link #rate} 输出「比例（非百分数，2 位小数）」，两者都对分母为 0 做了兜底，
 * 保证接口文档要求的「无数据时比率为 0.00」。</p>
 *
 * <p><b>设计 4 —— 关联名称统一批量装配。</b>
 * 类目名与维修人员姓名都采用「先聚合、后补装」的两段式，
 * 与模块二 ~ 模块六的做法一致，查询次数恒定、不随结果条数增长（避免 N+1）。</p>
 *
 * <h3>三、为什么本类没有任何 {@code @Transactional}？</h3>
 * <p>本模块是<b>纯只读的汇总模块</b>：五个方法全部只做 {@code SELECT}，
 * 不写任何业务数据，也不存在「多处写入需保持一致」的场景。
 * 因此不需要事务边界；保持只读也让数据库可以走从库、可以加缓存，
 * 是数据汇总类模块的标准做法。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    /** 统计聚合查询（本模块新建，只读） */
    private final StatisticsMapper statisticsMapper;

    /** 类目字典：类目名称装配（模块一既有 Mapper，本模块只读复用） */
    private final RepairCategoryMapper repairCategoryMapper;

    /** 用户表：维修人员姓名 / 工种装配（模块零既有 Mapper，本模块只读复用） */
    private final SysUserMapper sysUserMapper;

    /** 时间格式：与全局 JacksonConfig 保持一致 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(StatisticsConstants.TIME_PATTERN);

    /** 日期格式：查询条件允许只传日期，自动补全时分秒 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(StatisticsConstants.DATE_PATTERN);

    /** 时间轴标签格式（日 / 周） */
    private static final DateTimeFormatter LABEL_DAY_FORMATTER =
            DateTimeFormatter.ofPattern(StatisticsConstants.LABEL_PATTERN_DAY);

    /** 时间轴标签格式（月） */
    private static final DateTimeFormatter LABEL_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern(StatisticsConstants.LABEL_PATTERN_MONTH);

    // ==================================================================================
    // 一、S-01 统计概览看板
    // ==================================================================================

    /**
     * S-01 统计概览看板。
     *
     * <p>对应需求 FR-7-10；接口文档 4.8.1。</p>
     *
     * <p><b>口径落地要点</b>：</p>
     * <ol>
     *   <li>计数类 9 项由 {@code selectOverviewCounts} 一条 SQL 一次算出；
     *       {@code totalOrderCount} 已由 SQL 按 {@code NOT IN (7,9)} 排除无效工单（BR-7-04）；</li>
     *   <li>{@code todayOrderCount} 为独立查询（{@code create_time >= CURDATE()}），
     *       与所选时间范围无关，符合接口文档 4.8.1 的定义；</li>
     *   <li>{@code completedRate}（维修完成率）= 完成量(5+6) ÷ 有效报修量 × 100%，
     *       分子分母分别来自附录 C2 与 BR-7-04 的既有口径；</li>
     *   <li>{@code avgScore} 直接对 {@code repair_evaluation.total_score} 求平均，
     *       天然满足 BR-7-03「未评价工单不计入分母」，无评价时为 {@code null}；</li>
     *   <li>{@code evaluatedRate} = 已评价工单数 ÷ 完成量(5+6)，为<b>比例</b>而非百分数
     *       （接口文档 4.8.1 响应示例为 {@code 0.87}）；</li>
     *   <li>{@code avgAcceptMinutes} / {@code avgRepairMinutes} 按 BR-7-02 口径计算，
     *       无样本时统一返回 0（接口文档 4.8.1：「无数据时数值类返回 0」）。</li>
     * </ol>
     *
     * <p><b>时间范围</b>：{@code startTime} 缺省为<b>当年 1 月 1 日 00:00:00</b>、
     * {@code endTime} 缺省为<b>当前时间</b>（接口文档 4.8.1）。
     * 由于缺省跨度不超过一年，天然满足 BR-7-05 的 730 天上限；
     * 若显式传入超过 730 天的区间，仍返回 400。</p>
     */
    @Override
    public StatisticsOverviewVO getOverview(StatisticsQueryDTO query) {
        requireAdmin();
        StatisticsQueryDTO safeQuery = safe(query);
        // 注意：S-01 按接口文档 4.8.1 只接受 startTime / endTime，不读取类目与维修人员筛选
        LocalDateTime endTime = resolveEndTime(safeQuery, LocalDateTime.now());
        LocalDateTime startTime = resolveStartTime(safeQuery, defaultYearStart());
        checkRange(startTime, endTime);

        StatisticsOverviewVO overview = statisticsMapper.selectOverviewCounts(startTime, endTime);
        if (overview == null) {
            // 理论上不会发生（聚合查询恒返回一行），此处仅为极端情况兜底，避免 NPE
            overview = new StatisticsOverviewVO();
        }

        // ① 今日新增（独立口径，与所选范围无关）
        overview.setTodayOrderCount(nullToZero(statisticsMapper.countTodayOrders()));

        // ② 维修完成率 = 完成量(5+6) ÷ 有效报修量 × 100%
        long finishedCount = nullToZero(overview.getVerifiedCount()) + nullToZero(overview.getCompletedCount());
        overview.setCompletedRate(percentage(finishedCount, nullToZero(overview.getTotalOrderCount())));

        // ③ 平均评分与评价率（时间口径为「评价时间」）
        overview.setAvgScore(statisticsMapper.selectAvgScore(startTime, endTime));
        long evaluatedCount = nullToZero(statisticsMapper.countEvaluatedOrders(startTime, endTime));
        overview.setEvaluatedRate(rate(evaluatedCount, finishedCount));

        // ④ 平均接单时长 / 平均维修时长（BR-7-02）
        overview.setAvgAcceptMinutes(nullToZero(statisticsMapper.selectAvgAcceptMinutes(startTime, endTime)));
        overview.setAvgRepairMinutes(nullToZero(statisticsMapper.selectAvgRepairMinutes(startTime, endTime)));

        log.info("管理员 {} 查询统计概览：区间 [{} ~ {}]，有效报修量={}，完成率={}%，平均评分={}",
                UserContext.getUserId(), startTime, endTime,
                overview.getTotalOrderCount(), overview.getCompletedRate(), overview.getAvgScore());
        return overview;
    }

    // ==================================================================================
    // 二、S-02 报修量趋势统计
    // ==================================================================================

    /**
     * S-02 报修量趋势统计。
     *
     * <p>对应需求 FR-7-01；接口文档 4.8.2。</p>
     *
     * <p><b>时间轴补全的处理方式（本方法最需要解释的一点）</b>：
     * Mapper 只返回<b>有数据的时间点</b>（这是 SQL {@code GROUP BY} 的天然行为），
     * 若直接交给前端，折线图会在无数据的日期处断开。
     * 因此这里做两步：</p>
     * <ol>
     *   <li>用 {@code TreeSet} 合并「按粒度生成的完整时间轴」与「SQL 返回的时间点」，
     *       既保证连续、又保证升序，且不会丢掉任何异常落在轴外的点（防御性）；
     *       用 {@code TreeSet} 而不是手工排序，是因为标签格式统一为
     *       {@code yyyy-MM-dd} 或 {@code yyyy-MM}，<b>字典序恰好等于时间序</b>；</li>
     *   <li>轴上没有对应数据的点补一条「四项计数均为 0」的记录。</li>
     * </ol>
     * <p>但若区间内<b>一条工单都没有</b>（{@code rows} 为空），则按接口文档 4.8.2 的规定
     * 返回<b>空数组</b>与 {@code total = 0}，而不是返回一条全 0 的长轴——
     * 这样前端可以明确地展示「暂无数据」占位（需求文档 5.7.6 异常处理第 1 条）。</p>
     *
     * <p><b>total 口径</b>：等于各时间点 {@code orderCount} 之和，即区间内提交的工单总数
     * （含全部 10 种状态）。</p>
     */
    @Override
    public StatisticsTrendVO getTrend(StatisticsQueryDTO query) {
        requireAdmin();
        StatisticsQueryDTO safeQuery = safe(query);
        String granularity = normalizeGranularity(safeQuery.getGranularity());
        checkUrgency(safeQuery.getUrgency());

        LocalDateTime endTime = resolveEndTime(safeQuery, LocalDateTime.now());
        // 接口文档 4.8.2：startTime 缺省为「近 30 天」
        LocalDateTime startTime = resolveStartTime(safeQuery,
                endTime.minusDays(StatisticsConstants.DEFAULT_TREND_DAYS));
        checkRange(startTime, endTime);

        List<TrendItemVO> rows;
        if (StatisticsConstants.GRANULARITY_WEEK.equals(granularity)) {
            rows = statisticsMapper.selectTrendByWeek(startTime, endTime,
                    safeQuery.getCategoryId(), safeQuery.getWorkerId(), safeQuery.getUrgency());
        } else if (StatisticsConstants.GRANULARITY_MONTH.equals(granularity)) {
            rows = statisticsMapper.selectTrendByMonth(startTime, endTime,
                    safeQuery.getCategoryId(), safeQuery.getWorkerId(), safeQuery.getUrgency());
        } else {
            rows = statisticsMapper.selectTrendByDay(startTime, endTime,
                    safeQuery.getCategoryId(), safeQuery.getWorkerId(), safeQuery.getUrgency());
        }

        StatisticsTrendVO trend = new StatisticsTrendVO();
        trend.setGranularity(granularity);

        // 区间总量 = 各时间点工单数之和
        long total = 0L;
        for (TrendItemVO row : rows) {
            total += nullToZero(row.getOrderCount());
        }
        trend.setTotal(total);

        // 区间内无任何工单 → 空数组 + total = 0（接口文档 4.8.2 状态码表）
        if (rows.isEmpty()) {
            trend.setList(new ArrayList<>());
            return trend;
        }

        Map<String, TrendItemVO> rowMap = new LinkedHashMap<>();
        for (TrendItemVO row : rows) {
            rowMap.put(row.getTimeLabel(), row);
        }

        // 合并「完整时间轴」与「实际有数据的时间点」，字典序即时间序
        Set<String> labels = new TreeSet<>(buildTimeAxis(granularity, startTime.toLocalDate(), endTime.toLocalDate()));
        labels.addAll(rowMap.keySet());

        List<TrendItemVO> list = new ArrayList<>(labels.size());
        for (String label : labels) {
            TrendItemVO item = rowMap.get(label);
            if (item == null) {
                item = emptyTrendItem(label);
            }
            list.add(item);
        }
        trend.setList(list);

        log.info("管理员 {} 查询报修趋势：区间 [{} ~ {}]，粒度={}，时间点数={}，区间总量={}",
                UserContext.getUserId(), startTime, endTime, granularity, list.size(), total);
        return trend;
    }

    // ==================================================================================
    // 三、S-03 类目分布统计
    // ==================================================================================

    /**
     * S-03 类目分布统计。
     *
     * <p>对应需求 FR-7-02；接口文档 4.8.3。</p>
     *
     * <p><b>三个关键处理</b>：</p>
     * <ol>
     *   <li><b>分母独立查询</b>：{@code total} 由 {@code countValidOrders} 单独查出，
     *       <b>不能</b>用返回列表的工单量求和代替——因为列表受 {@code topN} 截断，
     *       求和会小于真实总量，导致各项占比整体偏大（这是统计类接口最常见的口径错误）；</li>
     *   <li><b>类目名装配</b>：一级聚合时把根类目名直接返回；二级聚合时拼成
     *       「父类目 / 子类目」（如「水电维修 / 电路故障」），
     *       与模块六 E-02 的 {@code categoryName} 拼装口径完全一致；</li>
     *   <li><b>占比计算</b>：{@code orderCount ÷ total × 100}，保留 2 位小数、HALF_UP；
     *       {@code total} 为 0 时各项统一返回 {@code 0.00}。</li>
     * </ol>
     *
     * <p><b>时间范围</b>：{@code startTime} 缺省为当年 1 月 1 日、{@code endTime} 缺省为当前时间
     * （接口文档 4.8.3 未规定默认值，此处与 S-01 保持一致的「本年度」口径，
     * 既能覆盖数据总量，又天然满足 BR-7-05 的跨度约束）。</p>
     */
    @Override
    public StatisticsCategoryVO getCategoryDistribution(StatisticsQueryDTO query) {
        requireAdmin();
        StatisticsQueryDTO safeQuery = safe(query);
        int level = normalizeLevel(safeQuery.getLevel());
        int topN = normalizeTopN(safeQuery.getTopN());

        LocalDateTime endTime = resolveEndTime(safeQuery, LocalDateTime.now());
        LocalDateTime startTime = resolveStartTime(safeQuery, defaultYearStart());
        checkRange(startTime, endTime);

        List<CategoryStatItemVO> rows = (level == StatisticsConstants.LEVEL_SECONDARY)
                ? statisticsMapper.selectCategoryStatLevel2(startTime, endTime,
                safeQuery.getCategoryId(), safeQuery.getWorkerId(), topN)
                : statisticsMapper.selectCategoryStatLevel1(startTime, endTime,
                safeQuery.getCategoryId(), safeQuery.getWorkerId(), topN);

        // 有效报修量：作为各项占比的统一分母（独立查询，不受 topN 影响）
        long total = nullToZero(statisticsMapper.countValidOrders(
                startTime, endTime, safeQuery.getCategoryId(), safeQuery.getWorkerId()));

        Map<Long, RepairCategory> categoryMap = loadCategoryMap();
        for (CategoryStatItemVO row : rows) {
            row.setCategoryName(categoryDisplayName(categoryMap, row.getCategoryId()));
            row.setRatio(percentage(nullToZero(row.getOrderCount()), total));
        }

        StatisticsCategoryVO result = new StatisticsCategoryVO();
        result.setTotal(total);
        result.setList(rows);

        log.info("管理员 {} 查询类目分布：区间 [{} ~ {}]，层级={}，topN={}，有效报修量={}，返回类目数={}",
                UserContext.getUserId(), startTime, endTime, level, topN, total, rows.size());
        return result;
    }

    // ==================================================================================
    // 四、S-04 工单状态分布统计
    // ==================================================================================

    /**
     * S-04 工单状态分布统计。
     *
     * <p>对应需求 FR-7-03；接口文档 4.8.4。</p>
     *
     * <p><b>「10 态完整输出」是怎么保证的？</b>
     * Mapper 只返回数据库里真实存在的状态，本方法改为以
     * {@link OrderStatusEnum#values()} <b>驱动遍历</b>（枚举声明顺序恰好是 0 → 9），
     * 逐项从计数 Map 中取值、取不到就填 0。
     * 这样做有三个好处：</p>
     * <ol>
     *   <li>状态名单的唯一权威仍在 Java 侧枚举，SQL 不承载状态清单，
     *       未来若状态字典变化，本方法自动跟随；</li>
     *   <li>天然满足接口文档 4.8.4「状态项按 0~9 升序输出，无数据时该项 orderCount 为 0」；</li>
     *   <li>不需要在 SQL 里写 {@code UNION} 十个 {@code SELECT} 来「造」空状态行。</li>
     * </ol>
     *
     * <p><b>占比分母</b>：统一为 {@code total}（有效报修量，不含已撤销 7 与已驳回 9）。
     * 即「已撤销」「已驳回」两项也以有效报修量为分母计算占比，
     * 以便直观看出无效工单相对有效工单的比例（与接口文档 4.8.4 响应示例的算法一致）。</p>
     */
    @Override
    public StatisticsStatusVO getStatusDistribution(StatisticsQueryDTO query) {
        requireAdmin();
        StatisticsQueryDTO safeQuery = safe(query);

        LocalDateTime endTime = resolveEndTime(safeQuery, LocalDateTime.now());
        LocalDateTime startTime = resolveStartTime(safeQuery, defaultYearStart());
        checkRange(startTime, endTime);

        List<StatusStatItemVO> rows = statisticsMapper.selectStatusDistribution(
                startTime, endTime, safeQuery.getCategoryId(), safeQuery.getWorkerId());

        Map<Integer, Long> countMap = new HashMap<>();
        for (StatusStatItemVO row : rows) {
            if (row.getStatus() != null) {
                countMap.put(row.getStatus(), nullToZero(row.getOrderCount()));
            }
        }

        // 有效报修量：占比的统一分母
        long total = nullToZero(statisticsMapper.countValidOrders(
                startTime, endTime, safeQuery.getCategoryId(), safeQuery.getWorkerId()));

        // 以 10 态枚举驱动遍历，保证输出完整且升序
        List<StatusStatItemVO> list = new ArrayList<>(OrderStatusEnum.values().length);
        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            StatusStatItemVO item = new StatusStatItemVO();
            item.setStatus(statusEnum.getCode());
            item.setStatusName(statusEnum.getName());
            long count = countMap.getOrDefault(statusEnum.getCode(), 0L);
            item.setOrderCount(count);
            item.setRatio(percentage(count, total));
            list.add(item);
        }

        StatisticsStatusVO result = new StatisticsStatusVO();
        result.setTotal(total);
        result.setList(list);

        log.info("管理员 {} 查询状态分布：区间 [{} ~ {}]，有效报修量={}，输出状态数={}",
                UserContext.getUserId(), startTime, endTime, total, list.size());
        return result;
    }

    // ==================================================================================
    // 五、S-05 维修人员工作量统计
    // ==================================================================================

    /**
     * S-05 维修人员工作量统计。
     *
     * <p>对应需求 FR-7-04；接口文档 4.8.5。</p>
     *
     * <p><b>两段式装配（先聚合、后补装）</b>：</p>
     * <ol>
     *   <li>{@code selectWorkerWorkload} 一条 SQL 算出每人的
     *       接单量 / 完成量 / 在途量 / 撤销量 / 平均维修时长，
     *       并按「完成量降序、接单量降序」排好名次（接口文档 4.8.5 的默认排序）；</li>
     *   <li>{@code selectWorkerAvgScore} 单独聚合出每人的平均评分，
     *       在内存里按 {@code workerId} 合并。<b>为什么不在第一条 SQL 里 JOIN 评价表？</b>
     *       因为两张表聚合粒度不同（工单 vs 评价），直接 JOIN 会在一人同时有 N 张工单与
     *       M 条评价时产生 N×M 行，把完成量算成 N×M 倍——这是统计 SQL 的经典陷阱；</li>
     *   <li>{@code sysUserMapper.selectBatchIds} 批量补齐姓名与工种，
     *       查询次数恒定为 1 次，不随人数增长（避免 N+1）。</li>
     * </ol>
     *
     * <p><b>时间范围</b>：与 S-03 / S-04 一致，{@code startTime} 缺省为当年 1 月 1 日。
     * 注意类目筛选作用于工单侧、时间筛选在评价侧同样生效，详见 Mapper 类注释「要点 4」。</p>
     *
     * <p><b>空结果处理</b>：区间内没有任何受派工单时返回空数组
     * （接口文档 4.8.5 状态码表：「无数据时返回空数组」），
     * 而不是返回一个「全部是 0」的假人员列表。</p>
     */
    @Override
    public List<WorkerStatVO> getWorkerWorkload(StatisticsQueryDTO query) {
        requireAdmin();
        StatisticsQueryDTO safeQuery = safe(query);

        LocalDateTime endTime = resolveEndTime(safeQuery, LocalDateTime.now());
        LocalDateTime startTime = resolveStartTime(safeQuery, defaultYearStart());
        checkRange(startTime, endTime);

        List<WorkerStatVO> rows = statisticsMapper.selectWorkerWorkload(
                startTime, endTime, safeQuery.getCategoryId(), safeQuery.getWorkerId());
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        // ① 合并平均评分（独立聚合，避免 JOIN 放大计数）
        Map<Long, BigDecimal> scoreMap = new HashMap<>();
        for (WorkerStatVO scoreRow : statisticsMapper.selectWorkerAvgScore(
                startTime, endTime, safeQuery.getCategoryId(), safeQuery.getWorkerId())) {
            if (scoreRow.getWorkerId() != null) {
                scoreMap.put(scoreRow.getWorkerId(), scoreRow.getAvgScore());
            }
        }

        // ② 批量补齐维修人员姓名与工种
        Set<Long> workerIds = new HashSet<>();
        for (WorkerStatVO row : rows) {
            if (row.getWorkerId() != null) {
                workerIds.add(row.getWorkerId());
            }
        }
        Map<Long, SysUser> userMap = loadUserMap(workerIds);

        for (WorkerStatVO row : rows) {
            SysUser worker = userMap.get(row.getWorkerId());
            if (worker != null) {
                row.setWorkerName(worker.getRealName());
                row.setWorkerType(worker.getWorkerType());
            }
            row.setAvgScore(scoreMap.get(row.getWorkerId()));
        }

        log.info("管理员 {} 查询维修人员工作量：区间 [{} ~ {}]，参与统计人数={}",
                UserContext.getUserId(), startTime, endTime, rows.size());
        return rows;
    }

    // ==================================================================================
    // 六、私有方法 —— 身份与权限
    // ==================================================================================

    /**
     * 取得当前登录管理员身份（BR-7-07 的服务层兜底防线）。
     *
     * <p>{@code @RequireRole(RoleEnum.ADMIN)} 已在 {@code JwtInterceptor} 层拦截非管理员，
     * 此处再校验一次，确保 Service 被其它入口（如未来的定时任务、单元测试）直接调用时
     * 依然不会泄漏统计数据（NFR-S-03）。</p>
     */
    private LoginUser requireAdmin() {
        LoginUser current = UserContext.require();
        if (!RoleEnum.ADMIN.getCode().equalsIgnoreCase(current.getRole())) {
            log.warn("非管理员越权访问统计接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, StatisticsConstants.MSG_ROLE_NOT_ADMIN);
        }
        return current;
    }

    // ==================================================================================
    // 七、私有方法 —— 查询参数解析与校验
    // ==================================================================================

    /** 空值安全的查询条件（前端完全不传参时 Controller 仍会传入一个空对象，此处再兜一层） */
    private StatisticsQueryDTO safe(StatisticsQueryDTO query) {
        return query == null ? new StatisticsQueryDTO() : query;
    }

    /** 解析「统计时间止」：未传时取传入的默认值（当前时间） */
    private LocalDateTime resolveEndTime(StatisticsQueryDTO query, LocalDateTime defaultValue) {
        LocalDateTime endTime = parseQueryTime(query.getEndTime(), true, "统计时间止");
        return endTime == null ? defaultValue : endTime;
    }

    /** 解析「统计时间起」：未传时取传入的默认值（当年 1 月 1 日 / 近 30 天） */
    private LocalDateTime resolveStartTime(StatisticsQueryDTO query, LocalDateTime defaultValue) {
        LocalDateTime startTime = parseQueryTime(query.getStartTime(), false, "统计时间起");
        return startTime == null ? defaultValue : startTime;
    }

    /** 当年 1 月 1 日 00:00:00（S-01 / S-03 / S-04 / S-05 的默认起始时间） */
    private LocalDateTime defaultYearStart() {
        return LocalDate.of(LocalDate.now().getYear(),
                StatisticsConstants.OVERVIEW_DEFAULT_START_MONTH,
                StatisticsConstants.OVERVIEW_DEFAULT_START_DAY).atStartOfDay();
    }

    /**
     * 解析时间查询参数，兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种写法。
     *
     * @param text       原始字符串
     * @param endOfDay   true 表示该参数是「止」（只传日期时补 23:59:59），false 表示「起」（补 00:00:00）
     * @param fieldLabel 字段中文名，用于拼装更精确的 400 提示
     * @return 解析后的时间；参数为空时返回 {@code null}
     */
    private LocalDateTime parseQueryTime(String text, boolean endOfDay, String fieldLabel) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            // 只传日期时按当日 00:00:00 / 23:59:59 处理，避免前端漏拼时分秒导致 400
            if (value.length() == StatisticsConstants.DATE_PATTERN.length()) {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "：" + StatisticsConstants.MSG_TIME_FORMAT);
        }
    }

    /**
     * 时间范围合法性校验（BR-7-05）。
     *
     * <p>两步：① 起不能晚于止；② 跨度不得超过 {@link StatisticsConstants#MAX_RANGE_DAYS} 天。
     * 提示语逐字对齐接口文档 4.8.2 的 400 说明。</p>
     */
    private void checkRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_TIME_ORDER_INVALID);
        }
        long days = ChronoUnit.DAYS.between(startTime, endTime);
        if (days > StatisticsConstants.MAX_RANGE_DAYS) {
            log.warn("统计时间跨度过大被拦截：{} 天（上限 {} 天）",
                    days, StatisticsConstants.MAX_RANGE_DAYS);
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_RANGE_TOO_LONG);
        }
    }

    /** 统计粒度规范化：未传取默认 {@code day}，非法值返回 400（接口文档 4.8.2） */
    private String normalizeGranularity(String granularity) {
        if (!StringUtils.hasText(granularity)) {
            return StatisticsConstants.GRANULARITY_DAY;
        }
        String value = granularity.trim().toLowerCase();
        if (!Arrays.asList(StatisticsConstants.GRANULARITIES).contains(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_GRANULARITY_INVALID);
        }
        return value;
    }

    /** 紧急程度取值域校验：仅允许 0（普通）/ 1（紧急），与模块二 2 级口径一致 */
    private void checkUrgency(Integer urgency) {
        if (urgency != null
                && urgency != com.community.repair.constant.OrderConstants.URGENCY_NORMAL
                && urgency != com.community.repair.constant.OrderConstants.URGENCY_URGENT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_URGENCY_INVALID);
        }
    }

    /** 类目聚合层级规范化：未传取默认一级，非法值返回 400（接口文档 4.8.3） */
    private int normalizeLevel(Integer level) {
        if (level == null) {
            return StatisticsConstants.LEVEL_PRIMARY;
        }
        if (level < StatisticsConstants.LEVEL_MIN || level > StatisticsConstants.LEVEL_MAX) {
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_LEVEL_INVALID);
        }
        return level;
    }

    /**
     * topN 规范化：未传取默认 10；小于 1 返回 400；大于上限按上限截断（不报错）。
     * <p>「超上限截断而不报错」是刻意的：topN 是展示参数而非业务参数，
     * 前端传 200 时给出「最多 50 条」比直接失败更友好。</p>
     */
    private int normalizeTopN(Integer topN) {
        if (topN == null) {
            return StatisticsConstants.DEFAULT_TOP_N;
        }
        if (topN < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, StatisticsConstants.MSG_TOP_N_INVALID);
        }
        return Math.min(topN, StatisticsConstants.MAX_TOP_N);
    }

    // ==================================================================================
    // 八、私有方法 —— 数值计算（占比 / 比率 / 装箱兜底）
    // ==================================================================================

    /** {@code Long} 拆箱兜底：{@code null} 一律按 0 处理，避免聚合结果为 NULL 时抛 NPE */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 百分数计算：{@code 分子 ÷ 分母 × 100}，保留 2 位小数、四舍五入（HALF_UP），单位为 %。
     *
     * <p>分母为 0 时返回 {@code 0.00}，落实接口文档 4.8.3 / 4.8.4「无数据时比率为 0.00」。
     * 使用 {@link BigDecimal} 而非 {@code double}，避免 {@code 0.1 + 0.2} 这类浮点误差
     * 让占比出现 44.029999999 的尾数。</p>
     */
    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0L) {
            return BigDecimal.ZERO.setScale(StatisticsConstants.RATIO_DECIMALS, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(StatisticsConstants.PERCENT_BASE)
                .divide(BigDecimal.valueOf(denominator),
                        StatisticsConstants.RATIO_DECIMALS, RoundingMode.HALF_UP);
    }

    /**
     * 比率计算：{@code 分子 ÷ 分母}，保留 2 位小数、四舍五入（HALF_UP），<b>结果为比例而非百分数</b>
     * （接口文档 4.8.1 的 {@code evaluatedRate} 响应示例为 {@code 0.87}）。
     */
    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0L) {
            return BigDecimal.ZERO.setScale(StatisticsConstants.RATE_DECIMALS, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator),
                        StatisticsConstants.RATE_DECIMALS, RoundingMode.HALF_UP);
    }

    // ==================================================================================
    // 九、私有方法 —— 趋势时间轴
    // ==================================================================================

    /**
     * 按粒度生成区间内的完整时间轴（标签格式与 Mapper 的 SQL 完全一致，才能对上）。
     *
     * <ul>
     *   <li>日：从起始日逐日推进到结束日；</li>
     *   <li>周：从起始日所在周的<b>周一</b>起，每 7 天推进一次，
     *       与 SQL 的 {@code DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY)} 对齐；</li>
     *   <li>月：从起始月逐月推进到结束月，标签为 {@code yyyy-MM}。</li>
     * </ul>
     *
     * <p>容量保护：日粒度下若区间接近 730 天上限，时间轴最多 731 个点，
     * 对 ECharts 折线图是可接受的量级（且 730 天已由 {@code checkRange} 强制封顶）。</p>
     */
    private List<String> buildTimeAxis(String granularity, LocalDate start, LocalDate end) {
        List<String> axis = new ArrayList<>();
        if (StatisticsConstants.GRANULARITY_MONTH.equals(granularity)) {
            YearMonth cursor = YearMonth.from(start);
            YearMonth last = YearMonth.from(end);
            while (!cursor.isAfter(last)) {
                axis.add(cursor.format(LABEL_MONTH_FORMATTER));
                cursor = cursor.plusMonths(1);
            }
            return axis;
        }
        if (StatisticsConstants.GRANULARITY_WEEK.equals(granularity)) {
            // DayOfWeek.MONDAY 基于 ISO 周字段，与 MySQL 的 WEEKDAY()（周一为 0）口径一致
            LocalDate cursor = start.with(DayOfWeek.MONDAY);
            while (!cursor.isAfter(end)) {
                axis.add(cursor.format(LABEL_DAY_FORMATTER));
                cursor = cursor.plusDays(7);
            }
            return axis;
        }
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            axis.add(cursor.format(LABEL_DAY_FORMATTER));
            cursor = cursor.plusDays(1);
        }
        return axis;
    }

    /** 构造一个「四项计数均为 0」的趋势点，用于补全时间轴上无数据的日期 */
    private TrendItemVO emptyTrendItem(String timeLabel) {
        TrendItemVO item = new TrendItemVO();
        item.setTimeLabel(timeLabel);
        item.setOrderCount(0L);
        item.setCompletedCount(0L);
        item.setCancelCount(0L);
        item.setRejectedCount(0L);
        return item;
    }

    // ==================================================================================
    // 十、私有方法 —— 关联数据批量装配
    // ==================================================================================

    /**
     * 一次性装载全部类目，构建「类目ID -> 类目实体」。
     *
     * <p>{@code repair_category} 是基础数据字典表（初始化仅 9 条，规模恒定在个位/十位数），
     * 一次全量取出即可同时完成「子类目 + 父类目名」的拼装，优于按 parentId 二次查询
     * （与模块二、模块六的做法完全一致）。</p>
     */
    private Map<Long, RepairCategory> loadCategoryMap() {
        Map<Long, RepairCategory> map = new HashMap<>();
        for (RepairCategory category : repairCategoryMapper.selectList(null)) {
            map.put(category.getCategoryId(), category);
        }
        return map;
    }

    /**
     * 类目展示名：二级类目拼成「父类目 / 子类目」，一级类目直接返回本身。
     * <p>口径与模块六 E-02 的 {@code categoryName} 完全一致，
     * 保证同一张工单在「评价管理」与「统计分析」两个页面上显示的类目名一模一样。</p>
     */
    private String categoryDisplayName(Map<Long, RepairCategory> categoryMap, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        RepairCategory category = categoryMap.get(categoryId);
        if (category == null) {
            return null;
        }
        Long parentId = category.getParentId();
        if (parentId != null && parentId != CategoryConstants.ROOT_PARENT_ID) {
            RepairCategory parent = categoryMap.get(parentId);
            if (parent != null) {
                return parent.getCategoryName() + " / " + category.getCategoryName();
            }
        }
        return category.getCategoryName();
    }

    /** 批量装载用户实体（维修人员姓名与工种装配用），查询次数恒定为 1 次 */
    private Map<Long, SysUser> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysUser> map = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            map.put(user.getUserId(), user);
        }
        return map;
    }
}
