package com.community.repair.dto;

import com.community.repair.constant.OrderConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 撤销工单请求参数（对应接口 O-05：PUT /api/orders/{orderId}/cancel）。
 *
 * <p>依据：接口文档 4.3.5；需求 FR-2-06、BR-2-07。</p>
 *
 * <p>撤销原因必填且长度 5~200 字，落库到 {@code repair_order.cancel_reason}
 * （字段类型 VARCHAR(255)，业务上收紧为 200）。</p>
 */
@Data
public class OrderCancelDTO implements Serializable {

    /** 撤销原因，必填，5~200 字 */
    @NotBlank(message = "请填写撤销原因")
    @Size(min = OrderConstants.CANCEL_REASON_MIN_LENGTH, max = OrderConstants.CANCEL_REASON_MAX_LENGTH,
            message = "撤销原因长度须为 5~200 个字符")
    private String cancelReason;
}
