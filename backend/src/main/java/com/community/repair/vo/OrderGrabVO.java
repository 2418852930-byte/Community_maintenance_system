package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抢单结果 VO（对应接口 D-07 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.7；需求 FR-3-08、BR-3-04、BR-3-08。</p>
 *
 * <p>抢单是「先到先得」的并发敏感操作：服务端用
 * {@code UPDATE repair_order SET status = 1, worker_id = ? WHERE order_id = ? AND status = 0}
 * 的条件更新做乐观锁，受影响行数为 0 即表示已被他人抢先，
 * 直接返回 409「该工单已被其他维修人员领取」。</p>
 *
 * <p>注意：抢单由维修人员发起，<b>不涉及派单管理员</b>，
 * 因此不写入 {@code dispatcher_id}（该字段语义为「派单管理员」，抢单场景保持为空）。</p>
 */
@Data
public class OrderGrabVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 1（已派单） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 抢单成功的维修人员 ID */
    private Long workerId;

    /** 锁定（抢单）时间 */
    private LocalDateTime dispatchTime;
}
