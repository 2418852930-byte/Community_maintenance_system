package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 拒单结果 VO（对应接口 D-09 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.9；需求 FR-3-10、BR-3-05、BR-3-06、BR-3-08。</p>
 *
 * <p>拒单后工单回退至 0（待派单）并<b>清空原受派人信息</b>，
 * 因此 {@code workerId} 在响应中固定为 {@code null}，
 * 前端据此可确认「该工单已回到待派单池，可重新派单」。</p>
 */
@Data
public class OrderRejectVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 0（待派单） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 清空后的维修人员 ID，固定为 null */
    private Long workerId;

    /** 生成的转派退回记录 ID（repair_record.record_id） */
    private Long recordId;
}
