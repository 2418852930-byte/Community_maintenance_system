package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 追加维修进度结果 VO（对应接口 R-02 的 data 结构）。
 *
 * <p>依据：接口文档 4.5.2；需求 FR-4-04、FR-4-05。</p>
 *
 * <p>追加进度<b>不改变工单状态</b>，因此 {@code orderStatus} 恒为追加前的状态
 * （2 已接单或 3 维修中），返回它是为了让前端无需二次请求即可刷新状态标签；
 * {@code totalCost} 为累加本次费用后的工单累计维修费用。</p>
 */
@Data
public class OrderRecordCreateVO implements Serializable {

    /** 新增记录 ID（repair_record.record_id） */
    private Long recordId;

    /** 所属工单 ID */
    private Long orderId;

    /** 记录类型 */
    private Integer recordType;

    /** 记录类型中文名 */
    private String recordTypeName;

    /** 记录发生时间 */
    private LocalDateTime recordTime;

    /** 工单当前状态（追加进度不改变状态，仍为 2 或 3） */
    private Integer orderStatus;

    /** 工单累计维修费用（本次费用已累加） */
    private BigDecimal totalCost;
}
