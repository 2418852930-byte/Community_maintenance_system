package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发布抢单池结果 VO（对应接口 D-05 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.5；需求 FR-3-05、BR-3-01、BR-3-08。</p>
 *
 * <p>发布动作<b>不改变工单状态</b>（保持 0 待派单），
 * 只是把工单标记为「已发布抢单池」，使其可被 D-06 检索到，
 * 并保留一条 {@code dispatch_type = 3} 的审计记录。</p>
 */
@Data
public class GrabPublishVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 是否已进入抢单池，固定 true */
    private Boolean grabEnabled;

    /** 工单状态，保持 0（待派单） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
