package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量派单请求参数（对应接口 D-03：PUT /api/orders/batch-dispatch）。
 *
 * <p>依据：接口文档 4.4.3；需求 FR-3-03、NFR-R-01。</p>
 *
 * <p>勾选多条「待派单（0）」工单，一次性指派给同一维修人员。
 * 单次最多 {@link DispatchConstants#BATCH_MAX_COUNT} 条；
 * 参与批量派单的工单状态必须<b>均为 0</b>，处于「待审核（8）」或其它状态的工单一律
 * 进入返回的失败明细 {@code failList}，不阻断其余工单的派单。</p>
 */
@Data
public class BatchDispatchDTO implements Serializable {

    /** 工单 ID 集合，必填，1~50 条 */
    @NotEmpty(message = "请至少选择一条工单")
    @Size(max = DispatchConstants.BATCH_MAX_COUNT, message = "批量派单的工单数量须为 1~50 条")
    private List<Long> orderIds;

    /** 目标维修人员 ID，必填，全批共用同一人 */
    @NotNull(message = "请选择维修人员")
    private Long workerId;

    /** 派单备注，全批共用，≤ 255 字 */
    @Size(max = DispatchConstants.DISPATCH_REMARK_MAX_LENGTH,
            message = "派单备注长度不能超过 255 个字符")
    private String dispatchRemark;
}
