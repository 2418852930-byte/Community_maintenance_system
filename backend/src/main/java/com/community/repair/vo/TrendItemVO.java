package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * S-02 报修量趋势统计 —— 时间序列元素（模块七）。
 *
 * <p>对应接口文档 4.8.2 的 {@code data.list} 元素结构、需求 FR-7-01
 * （按日 / 周 / 月统计工单提交数量，以折线图展示变化趋势）。</p>
 *
 * <p><b>本对象的双重身份</b>：既作为接口出参的时间序列元素，
 * 也作为 {@code StatisticsMapper#selectTrendByDay / ByWeek / ByMonth}
 * 的<b>聚合行承载对象</b>（三个方法共用本类型），
 * 因此字段名与 SQL 中的下划线别名一一对应。</p>
 *
 * <p><b>四个计数的口径关系（务必不要理解成互斥分组）</b>：</p>
 * <ul>
 *   <li>{@code orderCount}：该时间点上<b>提交的全部工单数</b>（含已撤销 7、已驳回 9、待审核 8），
 *       即 BR-7-01「按提交时间统计的报修量」本身；</li>
 *   <li>{@code completedCount}：其中的子集 —— 当前状态为 5 / 6 的数量（附录 C2 完成量口径）；</li>
 *   <li>{@code cancelCount}：其中的子集 —— 当前状态为 7 的数量；</li>
 *   <li>{@code rejectedCount}：其中的子集 —— 当前状态为 9 的数量（接口文档 4.8.2 V2.0 新增）。</li>
 * </ul>
 *
 * <p>因此 {@code orderCount ≥ completedCount + cancelCount + rejectedCount} 恒成立，
 * 差额为仍处于 8 / 0 / 1 / 2 / 3 / 4 等在途状态的工单。
 * 前端折线图以 {@code orderCount} 为主曲线，其余三条作为对比曲线。</p>
 */
@Data
public class TrendItemVO implements Serializable {

    /**
     * 时间轴标签。
     * <ul>
     *   <li>日粒度：{@code 2026-09-17}（{@code yyyy-MM-dd}）；</li>
     *   <li>周粒度：该周的<b>周一日期</b>，{@code 2026-09-14}（{@code yyyy-MM-dd}）；</li>
     *   <li>月粒度：{@code 2026-09}（{@code yyyy-MM}）。</li>
     * </ul>
     * 说明：周粒度的分组表达式为 {@code DATE_SUB(create_time, INTERVAL WEEKDAY(create_time) DAY)}，
     * 即把每一天归到它所在周的周一，从而保证「跨月的一周」不会被拆成两段。
     */
    private String timeLabel;

    /** 该时间点提交的工单总数（含全部 10 种状态） */
    private Long orderCount;

    /** 其中当前状态为「已验收(5) + 已完成(6)」的数量（完成量口径，附录 C2） */
    private Long completedCount;

    /** 其中当前状态为「已撤销(7)」的数量 */
    private Long cancelCount;

    /** 其中当前状态为「已驳回(9)」的数量 */
    private Long rejectedCount;
}
