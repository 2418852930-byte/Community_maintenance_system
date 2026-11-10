package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接单结果 VO（对应接口 D-08 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.8；需求 FR-3-09、FR-4-02、BR-3-08。</p>
 *
 * <p>接单是工单从「派单链路」正式切入「维修链路」的动作：
 * 状态由 1（已派单）置为 2（已接单），同时生成进度时间线的首个节点
 * （{@code repair_record.record_type = 1}），
 * {@code recordId} 即为该节点的主键，下游模块四的进度时间轴以此为起点。</p>
 */
@Data
public class OrderAcceptVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 2（已接单） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 生成的接单记录 ID（repair_record.record_id） */
    private Long recordId;
}
