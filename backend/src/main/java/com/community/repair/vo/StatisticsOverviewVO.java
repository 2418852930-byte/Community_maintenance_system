package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * S-01 统计概览看板 响应对象（模块七）。
 *
 * <p>对应接口文档 4.8.1、需求 FR-7-10（首页看板核心指标卡）。</p>
 *
 * <p><b>本对象的双重身份</b>：既作为 <b>接口出参</b>直接返回给前端，
 * 同时其中的 {@code 计数类字段} 也作为 {@code StatisticsMapper#selectOverviewCounts}
 * 的<b>聚合查询承载对象</b>（一条 SQL 用 {@code SUM(CASE WHEN ...)} 一次算出全部计数），
 * 因此计数类字段的命名与 SQL 中的下划线别名一一对应。
 * 派生类字段（{@code completedRate} / {@code avgScore} / {@code evaluatedRate} 等）
 * 由 Service 层在拿到聚合结果后再计算填充。
 * 这与模块三 {@code WorkerWorkloadStatVO}（内部聚合结果）拆成两个对象的做法略有不同，
 * 原因是本对象的字段在出参与聚合两个场景下<b>完全同构</b>，拆成两份反而会增加一倍的
 * 字段映射与拷贝代码，不利于维护。</p>
 *
 * <p><b>口径说明（逐条对齐 BR-7-01 ~ BR-7-04 与建表脚本附录 C）</b>：</p>
 * <ul>
 *   <li>{@code totalOrderCount}（有效报修量）= 全部 − 已撤销(7) − 已驳回(9)，<b>待审核(8) 计入</b>；</li>
 *   <li>{@code todayOrderCount} 为「今日新增」独立口径，与所选时间范围无关；</li>
 *   <li>{@code cancelCount} / {@code rejectedCount} <b>单独成列</b>，不参与有效报修量；</li>
 *   <li>{@code avgScore} 仅统计已产生评价的工单（BR-7-03），无评价时为 {@code null}；</li>
 *   <li>{@code completedRate} 为本模块按用户需求补充的派生指标，详见字段注释。</li>
 * </ul>
 */
@Data
public class StatisticsOverviewVO implements Serializable {

    // ==================================================================================
    // 一、工单总量与状态计数（由聚合 SQL 直接填充）
    // ==================================================================================

    /** 累计有效工单数：不含已撤销(7) 与已驳回(9)，<b>含待审核(8)</b>（BR-7-04 + 附录 C4） */
    private Long totalOrderCount;

    /** 今日新增工单数（含待审核）。口径：今日 00:00:00 之后提交的全部未删除工单，与所选时间范围无关 */
    private Long todayOrderCount;

    /** 待审核数量（状态 8）—— 即管理员审核工作台的待办量（接口文档 4.8.1 V2.0 新增项） */
    private Long pendingAuditCount;

    /** 待派单数量（状态 0） */
    private Long pendingDispatchCount;

    /** 处理中数量（状态 1 已派单 + 2 已接单 + 3 维修中 + 4 待验收） */
    private Long processingCount;

    /** 待验收数量（状态 4）—— 处理中的子集，单独暴露便于首页提醒（FR-5-06 关联） */
    private Long pendingVerifyCount;

    /** 已验收数量（状态 5）—— 「验收通过、等待评价」的工单，是完成量的组成部分之一 */
    private Long verifiedCount;

    /** 已完成数量（状态 6）—— 已评价，业务闭环结束 */
    private Long completedCount;

    /** 已撤销数量（状态 7）—— 单独成列，不计入有效报修量（BR-7-04） */
    private Long cancelCount;

    /** 已驳回数量（状态 9）—— 单独成列，不计入有效报修量（BR-7-04） */
    private Long rejectedCount;

    // ==================================================================================
    // 二、派生指标（由 Service 计算填充）
    // ==================================================================================

    /**
     * 维修完成率（单位：% ，保留 2 位小数）。
     *
     * <p><b>口径</b>：{@code (已验收(5) + 已完成(6)) ÷ 有效报修量 × 100}。</p>
     *
     * <p><b>为什么是这个口径？</b>分子采用建表脚本附录 C2 定义的「完成量 = 已验收(5) + 已完成(6)」，
     * 分母采用 BR-7-04 定义的「有效报修量 = 总数 − 已撤销(7) − 已驳回(9)」——
     * 两者都是项目文档中<b>已经定义好、不需要新造</b>的口径，
     * 因此本指标的分子分母都能在需求 / 数据库文档中逐条溯源，不存在自定义口径。</p>
     *
     * <p>注意：本字段是《01-需求规格说明书》5.7 节 FR-7-01 ~ FR-7-10 与
     * 《03-接口设计文档》4.8 节 S-01 ~ S-09 <b>均未定义</b>的派生指标，
     * 由本模块按交付要求新增，口径已如上固定并在此显式记录。</p>
     */
    private BigDecimal completedRate;

    /** 平均综合评分（保留 1 位小数）。仅统计已产生评价的工单（BR-7-03），无评价时为 {@code null} */
    private BigDecimal avgScore;

    /**
     * 平均维修时长（分钟，四舍五入取整）。
     * <p>口径依据 BR-7-02：维修时长 = 维修完工时间 − 开始维修时间，
     * 其中「开始维修时间」取 {@code repair_record} 中该工单<b>首条「到达现场(2)」记录的 record_time</b>
     * （与建表脚本附录 C6 完全一致）。样本仅含 {@code worker_id} 非空且两个时间点齐全的工单。</p>
     */
    private Long avgRepairMinutes;

    /**
     * 平均接单时长（分钟，四舍五入取整）。
     * <p>口径依据 BR-7-02：接单时长 = 接单时间 − 派单时间（{@code accept_time − dispatch_time}）。</p>
     */
    private Long avgAcceptMinutes;

    /**
     * 评价率（比例，非百分数，保留 2 位小数；接口文档 4.8.1 响应示例为 {@code 0.87}）。
     *
     * <p><b>口径</b>：{@code 已评价工单数 ÷ 完成量(5 + 6)}。</p>
     *
     * <p>说明：接口文档原文写作「已评价工单数 / 已完成工单数」。在本项目的状态机中，
     * 状态 6「已完成」的语义<b>恰好等价于「已评价」</b>（评价提交后 5 → 6，见 BR-6-04），
     * 若分母取「状态 6 的数量」则该指标恒等于 1，失去意义。
     * 因此按附录 C2 的「完成量 = 已验收(5) + 已完成(6)」取分母，
     * 指标实际含义为「<b>已完成维修的工单中，居民已完成评价的比例</b>」，
     * 与 FR-7-06 服务质量统计、模块六 E-02 的评价覆盖率口径一致。</p>
     */
    private BigDecimal evaluatedRate;
}
