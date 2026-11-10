package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 改派请求参数（对应接口 D-04：PUT /api/orders/{orderId}/reassign）。
 *
 * <p>依据：接口文档 4.4.4；需求 FR-3-04、BR-3-02、BR-3-08。</p>
 *
 * <p>对<b>已派单（1）且维修人员尚未接单</b>的工单重新指派他人。
 * 原受派信息被覆盖（{@code worker_id} / {@code dispatcher_id} /
 * {@code dispatch_time} / {@code dispatch_remark}），
 * 同时向 {@code repair_dispatch_log} 写入一条
 * {@code dispatch_type = 2} 的记录，并通过
 * {@code from_worker_id} / {@code to_worker_id} 成对还原改派链条。</p>
 *
 * <p>管理员禁用某维修人员账号后，其名下在途工单亦通过本接口改派他人（BR-8-19）。</p>
 */
@Data
public class OrderReassignDTO implements Serializable {

    /** 新维修人员 ID，关联 sys_user.user_id（role = repair 且 status = 1） */
    @NotNull(message = "请选择新的维修人员")
    private Long workerId;

    /** 改派说明，≤ 255 字；亦作为派单记录的备注（{@code remark}） */
    @Size(max = DispatchConstants.DISPATCH_REMARK_MAX_LENGTH,
            message = "改派说明长度不能超过 255 个字符")
    private String dispatchRemark;
}
