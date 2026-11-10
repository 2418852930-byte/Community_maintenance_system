package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开始维修结果 VO（对应接口 R-03 的 data 结构）。
 *
 * <p>依据：接口文档 4.5.3；需求 FR-4-03、BR-4-02。</p>
 *
 * <p>返回变更后的状态（固定 3 维修中）与新生成的「到达现场」记录 ID，
 * 前端可据此立即在进度时间轴上追加一个节点，无需重新拉取整个时间线。</p>
 */
@Data
public class OrderStartVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 3（维修中） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 生成的「到达现场」记录 ID（repair_record.record_id，record_type = 2） */
    private Long recordId;

    /** 变更时间 */
    private LocalDateTime updateTime;
}
