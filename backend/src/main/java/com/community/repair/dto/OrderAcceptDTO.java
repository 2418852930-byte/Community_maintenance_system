package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 接单请求参数（对应接口 D-08：PUT /api/orders/{orderId}/accept）。
 *
 * <p>依据：接口文档 4.4.8；需求 FR-3-09、FR-4-02、BR-3-08。</p>
 *
 * <p>维修人员确认接受分派：记录 {@code accept_time}，状态由 1（已派单）置为 2（已接单），
 * 自动写入一条类型为「接单」的维修记录（{@code repair_record.record_type = 1}）
 * 形成进度时间线的首个节点，并写入一条 {@code dispatch_type = 5} 的派单记录。</p>
 */
@Data
public class OrderAcceptDTO implements Serializable {

    /**
     * 接单备注，≤ 500 字。
     * <p>落库为 {@code repair_record.progress_content}；不填时使用默认文案
     * 「维修人员已确认接单」（{@link DispatchConstants#DEFAULT_ACCEPT_CONTENT}）。</p>
     */
    @Size(max = DispatchConstants.ACCEPT_REMARK_MAX_LENGTH,
            message = "接单备注长度不能超过 500 个字符")
    private String remark;
}
