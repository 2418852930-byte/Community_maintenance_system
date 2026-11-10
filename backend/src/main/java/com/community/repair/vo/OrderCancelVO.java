package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 撤销工单返回结果 VO（对应接口 O-05 的 data 结构）。
 *
 * <p>依据：接口文档 4.3.5。{@code status} 固定返回 7（已撤销）。</p>
 */
@Data
public class OrderCancelVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 7（已撤销） */
    private Integer status;

    /** 状态中文名，固定「已撤销」 */
    private String statusName;

    /** 撤销原因 */
    private String cancelReason;

    /** 变更时间 */
    private LocalDateTime updateTime;
}
