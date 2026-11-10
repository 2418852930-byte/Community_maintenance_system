package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 维修耗时统计 VO（对应接口 R-05 的 data 结构）。
 *
 * <p>依据：接口文档 4.5.5；需求 FR-4-08、BR-7-02（接口文档 Q6 已统一口径）。</p>
 *
 * <p><b>计算口径（BR-7-02）</b>：</p>
 * <ul>
 *   <li>派单时长 = 派单时间 − 提交时间；</li>
 *   <li>接单时长 = 接单时间 − 派单时间；</li>
 *   <li>维修时长 = 完工时间 − 开始维修时间（即首条「到达现场」记录的 {@code record_time}）；</li>
 *   <li>验收时长 = 验收时间 − 完工时间；</li>
 *   <li>全流程时长 = 终点时间（验收时间优先，其次完工时间）− 提交时间。</li>
 * </ul>
 *
 * <p>未流转到的节点其时间为 {@code null}，对应时长同样为 {@code null}；
 * 待审核(8) / 已驳回(9) 工单尚未派单，故各耗时项均为 {@code null}。</p>
 *
 * <p>本 VO 同时为统计分析模块（S-06 平均耗时）提供基础数据。</p>
 */
@Data
public class OrderDurationVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 提交时间 */
    private LocalDateTime createTime;

    /** 派单时间（最近一次） */
    private LocalDateTime dispatchTime;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 开始维修时间（首条「到达现场」记录时间，record_type = 2） */
    private LocalDateTime startTime;

    /** 完工时间 */
    private LocalDateTime finishTime;

    /** 验收时间（最近一次） */
    private LocalDateTime verifyTime;

    /** 派单时长（分钟）= 派单时间 − 提交时间 */
    private Long dispatchDurationMinutes;

    /** 接单时长（分钟）= 接单时间 − 派单时间 */
    private Long acceptDurationMinutes;

    /** 维修时长（分钟）= 完工时间 − 开始维修时间 */
    private Long repairDurationMinutes;

    /** 验收时长（分钟）= 验收时间 − 完工时间 */
    private Long verifyDurationMinutes;

    /** 全流程时长（分钟）= 终点时间（验收时间优先，其次完工时间）− 提交时间 */
    private Long totalDurationMinutes;
}
