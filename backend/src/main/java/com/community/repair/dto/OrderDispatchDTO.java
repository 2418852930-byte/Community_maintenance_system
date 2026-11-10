package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 派单请求参数（对应接口 D-02：PUT /api/orders/{orderId}/dispatch）。
 *
 * <p>依据：接口文档 4.4.2；需求 FR-3-02、BR-3-01、BR-3-03、BR-3-08。</p>
 *
 * <p>派单是「待审核工单提交模块」与「维修进度管理模块」之间的关键衔接点：
 * 本对象落库后，{@code repair_order.worker_id} 即被关联，
 * 工单状态由 0（待派单）置为 1（已派单），
 * <b>数据结构完全满足下游模块四的输入要求</b>，无需任何适配。</p>
 *
 * <p>{@code dispatcherId} / {@code dispatchTime} <b>不接受前端传入</b>，
 * 一律由服务端从 JWT 解析的操作人身份与系统当前时间写入（NFR-S-03）。</p>
 */
@Data
public class OrderDispatchDTO implements Serializable {

    /** 目标维修人员 ID，关联 sys_user.user_id（role = repair 且 status = 1） */
    @NotNull(message = "请选择维修人员")
    private Long workerId;

    /** 派单备注，给维修人员的说明，≤ 255 字；不填则落库为 null */
    @Size(max = DispatchConstants.DISPATCH_REMARK_MAX_LENGTH,
            message = "派单备注长度不能超过 255 个字符")
    private String dispatchRemark;
}
