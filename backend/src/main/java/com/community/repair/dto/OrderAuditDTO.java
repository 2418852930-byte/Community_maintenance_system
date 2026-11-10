package com.community.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单审核请求参数（对应接口 D-12：PUT /api/orders/{orderId}/audit）。
 *
 * <p>依据：接口文档 4.4.12；需求 FR-3-12、BR-3-01、BR-3-09、BR-3-10。</p>
 *
 * <p>本对象承载「审核通过」与「审核驳回」两个互斥分支：</p>
 * <ul>
 *   <li>{@code auditResult = 1}（审核通过）：状态 8 → 0，{@code rejectReason} 被忽略；</li>
 *   <li>{@code auditResult = 0}（审核驳回）：状态 8 → 9，{@code rejectReason} <b>必填</b>，
 *       长度 5~200 字（BR-3-09）。</li>
 * </ul>
 *
 * <p><b>校验分层约定</b>（与模块一 / 模块二一致）：本类只做「必填 + 长度」的第一层校验，
 * 取值域（0 / 1）与「驳回必填」这类<b>跨字段的业务语义</b>放在 Service 层做第二层校验，
 * 以保证提示语与接口文档逐字一致。</p>
 */
@Data
public class OrderAuditDTO implements Serializable {

    /**
     * 审核结果：1-审核通过，0-审核驳回。
     * <p>必填；取值域校验由 Service 层完成（提示语对齐接口文档 4.4.12 的 400 说明）。</p>
     */
    @NotNull(message = "请选择审核结果")
    private Integer auditResult;

    /**
     * 驳回原因 / 审核意见，落库到 {@code repair_order.reject_reason}。
     * <p>{@code auditResult = 0} 时必填且为 5~200 字；{@code auditResult = 1} 时该字段被忽略。</p>
     */
    private String rejectReason;
}
