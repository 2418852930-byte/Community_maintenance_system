package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 拒单请求参数（对应接口 D-09：PUT /api/orders/{orderId}/reject）。
 *
 * <p>依据：接口文档 4.4.9；需求 FR-3-10、BR-3-05、BR-3-06、BR-3-08。</p>
 *
 * <p>维修人员拒绝接单后，工单回退至状态 0（待派单）并清空原受派人信息，
 * 同时留存：</p>
 * <ul>
 *   <li>一条类型为「转派退回」的维修记录（{@code repair_record.record_type = 6}）；</li>
 *   <li>一条 {@code dispatch_type = 6} 的派单记录，拒单原因写入其 {@code remark}。</li>
 * </ul>
 *
 * <p><b>语义区分提醒</b>（接口文档 Q9）：本接口的「拒单」是<b>维修人员行为</b>，
 * 与 D-12 管理员「审核驳回」不是同一环节，两者字段与状态迁移刻意区分。</p>
 */
@Data
public class OrderRejectDTO implements Serializable {

    /** 拒单原因，必填，5~200 字（BR-3-05） */
    @NotBlank(message = "请填写拒单原因")
    @Size(min = DispatchConstants.REJECT_ORDER_REASON_MIN_LENGTH,
            max = DispatchConstants.REJECT_ORDER_REASON_MAX_LENGTH,
            message = "拒单原因长度须为 5~200 个字符")
    private String rejectReason;
}
