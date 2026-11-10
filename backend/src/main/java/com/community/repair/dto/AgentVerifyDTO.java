package com.community.repair.dto;

import com.community.repair.constant.VerifyConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员代验收请求参数（对应接口 V-03：PUT /api/orders/{orderId}/verify-agent）。
 *
 * <p>依据：接口文档 4.6.3；需求 FR-5-07、BR-5-07、BR-5-08。</p>
 *
 * <p><b>为什么只有一个字段？</b>因为本接口<b>仅支持验收通过，不支持代驳回</b>
 * （接口文档 4.6.3 明确规定）。也就是说 {@code verifyResult} 恒为 1、目标状态恒为 5，
 * 两者都不需要由前端传入 —— 少一个可传参数，就少一条可能被绕过的越权路径。</p>
 *
 * <p><b>BR-5-07「必须标识操作类型并填写说明」的落地方式</b>：</p>
 * <ul>
 *   <li>「标识操作类型」→ 落库时 {@code repair_verify_log.verify_type = 2}（管理员代验收），
 *       与居民验收（{@code verify_type = 1}）在数据层面永久区分，V-04 可直接对外展示；</li>
 *   <li>「填写说明」→ 本 DTO 的 {@code verifyRemark} 为 {@code @NotBlank} 且 5~200 字，
 *       Service 层再做一次长度与非空格校验。</li>
 * </ul>
 *
 * <p><b>调用前提</b>：工单必须已超过 {@link VerifyConstants#OVERDUE_HOURS} 小时
 * 的待验收超时阈值（BR-5-08，与 V-01 的 {@code verifyOverdue} 同一阈值），
 * 未达阈值时返回 409。</p>
 */
@Data
public class AgentVerifyDTO implements Serializable {

    /**
     * 代验收说明，<b>必填</b> 5~200 字（BR-5-07）。
     *
     * <p>该文本会原样落库到 {@code repair_verify_log.verify_remark}，
     * 并在 V-04 验收记录查询中与 {@code verifyTypeName = "管理员代验收"} 一同展示，
     * 使「谁在什么情况下代为验收」全程可追溯。</p>
     */
    @NotBlank(message = "代验收说明不能为空")
    @Size(min = VerifyConstants.AGENT_REMARK_MIN_LENGTH,
            max = VerifyConstants.AGENT_REMARK_MAX_LENGTH,
            message = "代验收说明须为 5~200 个字符")
    private String verifyRemark;
}
