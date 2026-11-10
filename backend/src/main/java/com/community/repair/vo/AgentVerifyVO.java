package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员代验收结果 VO（对应接口 V-03 的 data 结构）。
 *
 * <p>依据：接口文档 4.6.3 的「返回参数（data）」表格，字段逐一对齐。</p>
 *
 * <p><b>与 {@link VerifyResultVO} 的差异（为何不合并为一个 VO）</b>：
 * 代验收是<b>只有通过、没有驳回</b>的动作，因此不需要 {@code verifyResultName} /
 * {@code canEvaluate} 这类「结果分支」字段；反过来它必须暴露
 * {@code verifyType}（固定 2）与 {@code operatorName}，以落实 BR-5-07
 * 「代验收必须标识操作类型」的可追溯要求。
 * 两个接口的出参结构本就不同，合并会让调用方拿到一堆恒 null 的字段，
 * 违背「出参与接口语义严格对应」的设计原则。</p>
 */
@Data
public class AgentVerifyVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态，固定 5（已验收） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 验收结果，固定 1（通过）—— 本接口不支持代驳回 */
    private Integer verifyResult;

    /** 本轮验收轮次（写入 repair_verify_log.verify_round） */
    private Integer verifyRound;

    /** 验收时间（写入 repair_order.verify_time 与 repair_verify_log.verify_time） */
    private LocalDateTime verifyTime;

    /** 代验收说明（BR-5-07 必填，原样回显） */
    private String verifyRemark;

    /** 验收类型，固定 2（管理员代验收），与居民验收（1）在数据层面永久区分 */
    private Integer verifyType;

    /** 操作管理员姓名（audit 字段，便于前端提示「已完成代验收」） */
    private String operatorName;
}
