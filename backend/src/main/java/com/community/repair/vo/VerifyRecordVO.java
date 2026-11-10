package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验收记录 VO（对应接口 V-04 的 data 数组元素）。
 *
 * <p>依据：接口文档 4.6.4 的「返回参数（data，数组，按 verifyRound 升序）」表格，
 * 字段逐一对齐；数据来源为 {@code repair_verify_log} 表。</p>
 *
 * <p><b>本 VO 存在的意义是「多轮留痕可见」</b>：一个工单可能经历
 * 「验收驳回 → 返修 → 再次验收通过」多轮（BR-5-05），
 * {@code repair_order} 只保存最近一次的快照，无法还原这个过程。
 * 本 VO 按 {@code verifyRound} 升序返回每一轮，使居民、维修人员与管理员都能看到
 * 「第几轮、谁验的、什么结果、驳回原因是什么」，这是 BR-5-05 的最终呈现形态。</p>
 *
 * <p><b>数据范围</b>：V-04 标注为「全部登录用户（受数据范围约束）」，
 * 与 O-03 工单详情一致 —— 居民仅本人工单、维修人员仅本人受派工单、管理员全部，
 * 不符时返回 403（在 Service 层强制校验）。</p>
 */
@Data
public class VerifyRecordVO implements Serializable {

    /** 验收记录 ID（repair_verify_log.verify_id） */
    private Long verifyId;

    /** 所属工单 ID */
    private Long orderId;

    /** 验收轮次，自 1 起递增（order_id + verify_round 联合唯一） */
    private Integer verifyRound;

    /** 验收类型：1 居民验收 / 2 管理员代验收 */
    private Integer verifyType;

    /** 验收类型中文名 */
    private String verifyTypeName;

    /** 验收结果：0 驳回 / 1 通过 */
    private Integer verifyResult;

    /** 验收结果中文名 */
    private String verifyResultName;

    /** 验收意见 / 驳回原因 / 代验收说明 */
    private String verifyRemark;

    /** 验收人 ID（居民或管理员，关联 sys_user.user_id） */
    private Long verifierId;

    /** 验收人姓名 */
    private String verifierName;

    /**
     * 验收人角色编码：{@code resident} 或 {@code admin}。
     *
     * <p>与 {@code verifyType} 互为印证：{@code type = 1} 时角色通常为 resident，
     * {@code type = 2} 时为 admin。保留角色编码是为了让前端能够
     * 在不额外查用户接口的前提下，正确渲染「管理员 张三（代验收）」这类文案。</p>
     */
    private String verifierRole;

    /** 验收时间 */
    private LocalDateTime verifyTime;
}
