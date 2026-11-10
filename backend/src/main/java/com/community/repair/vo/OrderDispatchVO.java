package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单结果 VO（对应接口 D-02 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.2；需求 FR-3-02、BR-3-01、BR-3-03、BR-3-08。</p>
 *
 * <p><b>本对象是模块三向下游模块四交接的「数据契约」</b>：
 * 派单成功后 {@code repair_order} 中写入的
 * {@code worker_id} / {@code dispatcher_id} / {@code dispatch_time} /
 * {@code dispatch_remark} 与此处返回字段一一对应，
 * 工单状态固定为 1（已派单），下游「维修进度管理模块」据此即可
 * 在维修人员的「我的工单」中检索到该工单并发起接单，
 * <b>无需对本模块的数据结构做任何适配</b>。</p>
 */
@Data
public class OrderDispatchVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 变更后状态，固定 1（已派单） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 维修人员 ID */
    private Long workerId;

    /** 维修人员姓名 */
    private String workerName;

    /** 派单管理员 ID */
    private Long dispatcherId;

    /** 派单时间 */
    private LocalDateTime dispatchTime;
}
