package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * S-05 维修人员工作量统计 —— 单个维修人员行（模块七）。
 *
 * <p>对应接口文档 4.8.5 的 {@code data} 数组元素结构、需求 FR-7-04
 * （统计各维修人员的接单量、完成量、在途量）。</p>
 *
 * <p><b>本对象的双重身份</b>：既作为接口出参，也作为
 * {@code StatisticsMapper#selectWorkerWorkload} 的聚合行承载对象；
 * {@code avgScore} 由 {@code StatisticsMapper#selectWorkerAvgScore} 单独聚合后，
 * 在 Service 层按 {@code workerId} 合并；
 * {@code workerName} / {@code workerType} 由 Service 层从模块零的 {@code sys_user}
 * 批量装配（沿用模块三 D-01 的两段式做法：先聚合、后补装档案，避免 SQL 里出现多表 JOIN 与 N+1）。</p>
 *
 * <p><b>四项计数的口径（严格对齐建表脚本附录 C2，接口文档 4.8.5 V2.0 口径说明）</b>：</p>
 * <ul>
 *   <li>{@code acceptCount} 接单量 = 累计受派工单数。判定依据是 {@code repair_order.worker_id = 该人}，
 *       <b>不论工单当前处于什么状态</b>（含被撤销的）；</li>
 *   <li>{@code finishCount} 完成量 = 已验收(5) + 已完成(6)；</li>
 *   <li>{@code processingCount} 在途量 = 已派单(1) + 已接单(2) + 维修中(3) + 待验收(4)；</li>
 *   <li>{@code cancelCount} 撤销量 = 受派后工单被撤销的数量（状态 7）。</li>
 * </ul>
 *
 * <p><b>关键排除项</b>：待审核(8) / 已驳回(9) / 已撤销(7) 三类工单
 * 因 {@code worker_id} 恒为空（尚未进入派单链路），<b>天然不会出现在任何人员的指标里</b>——
 * 这不是靠额外条件过滤实现的，而是由状态机本身保证的，
 * 与本项目「审核环节不写入派单日志」的设计（接口文档 4.4 节 V2.0 变更说明）互为印证。</p>
 */
@Data
public class WorkerStatVO implements Serializable {

    /** 维修人员 ID（{@code repair_order.worker_id}） */
    private Long workerId;

    /** 维修人员真实姓名（取自 {@code sys_user.real_name}） */
    private String workerName;

    /** 维修工种（取自 {@code sys_user.worker_type}，如「水暖工」「电工」） */
    private String workerType;

    /** 接单量：累计受派工单数 */
    private Long acceptCount;

    /** 完成量：状态 5（已验收）+ 6（已完成） */
    private Long finishCount;

    /** 在途量：状态 1（已派单）+ 2（已接单）+ 3（维修中）+ 4（待验收） */
    private Long processingCount;

    /** 撤销量：受派后被撤销的工单数（状态 7） */
    private Long cancelCount;

    /**
     * 平均维修时长（分钟，四舍五入取整）。
     * <p>口径依据 BR-7-02：维修时长 = 维修完工时间 − 首条「到达现场(2)」记录时间。
     * 该人员没有任何「到达现场 + 完工」配对齐全的工单时为 {@code null}。</p>
     */
    private Long avgRepairMinutes;

    /**
     * 平均综合评分（保留 1 位小数）。
     * <p>依据 BR-7-03，仅统计已产生评价的工单（{@code repair_evaluation.worker_id = 该人}），
     * 未评价的工单不计入分母；该人员没有任何评价时为 {@code null}。</p>
     */
    private BigDecimal avgScore;
}
