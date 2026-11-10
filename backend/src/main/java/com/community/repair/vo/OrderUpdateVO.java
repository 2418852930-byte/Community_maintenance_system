package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改工单返回结果 VO（对应接口 O-04 的 data 结构）。
 *
 * <p>依据：接口文档 4.3.4。{@code status} 与修改前一致（8 → 8 或 0 → 0），
 * 修改<b>不改变工单状态</b>、不触发重新审核。</p>
 */
@Data
public class OrderUpdateVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 工单状态，与修改前一致（8 或 0） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
