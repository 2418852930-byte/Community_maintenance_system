package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单审核结果 VO（对应接口 D-12 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.12；需求 FR-3-12、BR-3-01、BR-3-09、BR-3-10。</p>
 *
 * <p><b>字段取值随审核结果分叉</b>：</p>
 * <ul>
 *   <li><b>审核通过</b>（{@code auditResult = 1}）：{@code status = 0}，
 *       {@code rejectReason} / {@code rejectorId} / {@code rejectorName} /
 *       {@code rejectTime} <b>四个字段一律为 {@code null}</b>；</li>
 *   <li><b>审核驳回</b>（{@code auditResult = 0}）：{@code status = 9}，
 *       上述四个字段全部有值。</li>
 * </ul>
 *
 * <p>该分叉口径与数据库 V2.0「驳回三要素仅状态 9 时有值」的约束严格一致，
 * 也与模块二 {@code OrderDetailVO} 的出口收敛逻辑互为呼应。</p>
 */
@Data
public class OrderAuditVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 审核结果：1 通过 / 0 驳回 */
    private Integer auditResult;

    /** 审核结果中文名：审核通过 / 审核驳回 */
    private String auditResultName;

    /** 变更后状态：通过为 0（待派单），驳回为 9（已驳回） */
    private Integer status;

    /** 变更后状态中文名 */
    private String statusName;

    /** 驳回原因（审核通过时为 null） */
    private String rejectReason;

    /** 驳回管理员 ID（审核通过时为 null） */
    private Long rejectorId;

    /** 驳回管理员姓名（审核通过时为 null；联查 sys_user.real_name 派生，不落库） */
    private String rejectorName;

    /** 驳回时间（审核通过时为 null） */
    private LocalDateTime rejectTime;

    /** 审核时间（即工单本次更新时间） */
    private LocalDateTime updateTime;
}
