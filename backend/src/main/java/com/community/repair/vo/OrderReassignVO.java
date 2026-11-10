package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 改派结果 VO（对应接口 D-04 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.4；需求 FR-3-04、BR-3-02、BR-3-08、BR-8-19。</p>
 *
 * <p>改派<b>不产生状态迁移</b>（始终保持 1 已派单，等待新维修人员接单），
 * 只覆盖当前有效派单快照并追加一条 {@code dispatch_type = 2} 的审计记录。</p>
 */
@Data
public class OrderReassignVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 状态，保持 1（已派单） */
    private Integer status;

    /** 新维修人员 ID */
    private Long workerId;

    /** 新维修人员姓名 */
    private String workerName;

    /** 重新派单时间 */
    private LocalDateTime dispatchTime;
}
