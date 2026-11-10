package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布抢单池请求参数（对应接口 D-05：PUT /api/orders/{orderId}/publish-grab）。
 *
 * <p>依据：接口文档 4.4.5；需求 FR-3-05、BR-3-01、BR-3-08。</p>
 *
 * <p>把「待派单（0）」工单发布为可抢单状态，由维修人员自行领取。
 * 发布后工单<b>仍保持状态 0</b>（不产生状态迁移），
 * 但会被 D-06 抢单池列表检索到，并向 {@code repair_dispatch_log}
 * 写入一条 {@code dispatch_type = 3} 的记录。</p>
 *
 * <p><b>「已发布抢单池」的判定依据</b>：{@code repair_order} 表未设专门的发布标记字段，
 * 依据接口文档 Q3「派单轨迹唯一来源 = {@code repair_dispatch_log}」的口径，
 * 服务端以「存在 {@code dispatch_type = 3} 且其后不存在 {@code dispatch_type ∈ (1,2,4)} 的记录」
 * 判定工单当前是否在抢单池中（详见 {@code RepairDispatchServiceImpl#isInGrabPool}）。</p>
 */
@Data
public class GrabPublishDTO implements Serializable {

    /** 发布说明，≤ 255 字；不填则落库为 null */
    @Size(max = DispatchConstants.DISPATCH_REMARK_MAX_LENGTH,
            message = "发布说明长度不能超过 255 个字符")
    private String dispatchRemark;
}
