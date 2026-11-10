package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 驳回工单修改后重新提交返回结果 VO（对应接口 O-06 的 data 结构）。
 *
 * <p>依据：接口文档 4.3.6（V2.0 新增）。{@code status} 固定返回 8（待审核），
 * 且 {@code rejectReason} / {@code rejectTime} 均为 {@code null}
 * —— 这体现「提交成功后清空驳回快照」的语义，与数据库 V2.0
 * 「驳回字段仅状态 9 时有值」的口径保持一致（BR-3-10）。</p>
 */
@Data
public class OrderResubmitVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 变更后状态，固定 8（待审核） */
    private Integer status;

    /** 状态中文名，固定「待审核」 */
    private String statusName;

    /** 驳回原因，清空后为 null */
    private String rejectReason;

    /** 驳回时间，清空后为 null */
    private LocalDateTime rejectTime;

    /** 重新提交时间 */
    private LocalDateTime updateTime;
}
