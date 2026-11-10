package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回复评价结果 VO（对应接口 E-04 的 data 结构）。
 *
 * <p>依据：接口文档 4.7.4 的「返回参数（data）」表格，字段逐一对齐。</p>
 *
 * <p><b>关于 {@code replyAdminId} / {@code replyAdminName}（重要实现说明）</b>：
 * 接口文档列出了这两个字段，但 {@code repair_evaluation} 表在方案 A 中
 * <b>只设计了 {@code reply_content} 与 {@code reply_time} 两列</b>，没有「回复管理员」列。
 * 本模块严格遵守「不改表结构」的约束，因此这两个字段的取值口径为
 * <b>「本次执行回复操作的管理员」</b>（即 {@code UserContext} 中当前登录的管理员），
 * 用于前端即时回显「谁回复的」。
 * 该口径属于「接口出参与表结构的最小化对齐」，不新增列、不新增表，
 * 已在 README 的「需求之外的功能说明」中明确记录。</p>
 */
@Data
public class EvaluationReplyVO implements Serializable {

    /** 评价 ID */
    private Long evaluationId;

    /** 回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 回复管理员 ID（取值口径：本次执行回复操作的管理员） */
    private Long replyAdminId;

    /** 回复管理员姓名（取值口径：本次执行回复操作的管理员） */
    private String replyAdminName;
}
