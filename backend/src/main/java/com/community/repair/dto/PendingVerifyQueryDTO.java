package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 待验收工单查询条件（对应接口 V-01：GET /api/orders/pending-verify）。
 *
 * <p>依据：接口文档 4.6.1；需求 FR-5-01、FR-5-06。</p>
 *
 * <p>本接口「等价于 {@code GET /api/orders?status=4}」，但作为<b>语义化快捷查询接口</b>
 * 单独提供，以便承载 O-02 不具备的两项验收专属能力：</p>
 * <ol>
 *   <li>{@code onlyOverdue} —— 一键筛出超时未验收的工单（FR-5-06 超时提醒）；</li>
 *   <li>{@code startTime} / {@code endTime} —— <b>按完工时间</b>（而非提交时间）筛选，
 *       这正是验收场景最自然的视角：居民关心的是「哪几天修好的工单我还没验收」。</li>
 * </ol>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：{@code pageNum} 从 1 开始，{@code pageSize} 最大 100。</p>
 *
 * <p><b>状态为什么不作为入参？</b>本接口语义已固定为「待验收」，工单状态由服务端
 * 强制注入 {@code status = 4}，前端无法通过传参改成查询其它状态 ——
 * 这既对齐接口文档的接口语义，也避免出现「用待验收列表接口查已撤销工单」这类越界用法。</p>
 */
@Data
public class PendingVerifyQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = 10;

    /** 报修类目ID，可选（接口文档 4.6.1） */
    private Long categoryId;

    /**
     * 是否仅返回超时未验收的工单，可选。
     *
     * <p>{@code true} 时仅返回「完工时间距当前已超过
     * {@code OrderConstants.VERIFY_OVERDUE_HOURS}（48 小时）」的工单，
     * 即 {@code verifyOverdue = true} 的那批（FR-5-06）。</p>
     */
    private Boolean onlyOverdue;

    /** 完工时间起，格式 yyyy-MM-dd HH:mm:ss（允许只传 yyyy-MM-dd，自动补 00:00:00） */
    private String startTime;

    /** 完工时间止，格式 yyyy-MM-dd HH:mm:ss（允许只传 yyyy-MM-dd） */
    private String endTime;
}
