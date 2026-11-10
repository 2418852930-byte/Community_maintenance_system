package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交维修完成结果 VO（对应接口 R-04 的 data 结构）。
 *
 * <p>依据：接口文档 4.5.4；需求 FR-4-06、BR-4-04。</p>
 *
 * <p><b>本 VO 是模块四交付给模块五的「验收入口契约」</b>：
 * {@code status = 4}（待验收）、{@code finishTime}、{@code totalCost}
 * 三个字段即构成 {@code repair_order} 表在「待验收」状态下的完整快照，
 * 下游模块五的待验收列表（V-01）与验收操作（V-02 / V-03）直接读取这三个字段，
 * <b>无需任何适配转换</b>。</p>
 */
@Data
public class OrderFinishVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 4（待验收） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 完工时间（写入 repair_order.finish_time） */
    private LocalDateTime finishTime;

    /** 维修费用合计（本次完工费用已累加） */
    private BigDecimal totalCost;

    /** 生成的完工记录 ID（repair_record.record_id，record_type = 5） */
    private Long recordId;
}
