package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评价分页查询条件（对应接口 E-02：GET /api/evaluations）。
 *
 * <p>依据：接口文档 4.7.2；需求 FR-6-04（我的评价查看）、FR-6-05（评价列表查询，支持星级 /
 * 时间 / 类目 / 维修人员筛选）、FR-6-07（维修人员评价查看）。</p>
 *
 * <p><b>数据范围由服务端按角色强制注入，前端无法绕过</b>：</p>
 * <ul>
 *   <li>居民：强制附加 {@code resident_id = 当前用户ID} —— 「我的评价」；</li>
 *   <li>维修人员：强制附加 {@code worker_id = 当前用户ID} —— 「本人所接工单收到的评价」；</li>
 *   <li>系统管理员：不限定，可看全部评价。</li>
 * </ul>
 *
 * <p><b>筛选参数的权限约束（接口文档 4.7.2 明确标注「仅管理员可用」）</b>：
 * {@code workerId} 与 {@code categoryId} 为管理员专属筛选维度，
 * 非管理员传入将返回 403（与服务端「居民 / 维修人员本就只能看自己范围数据」的设计一致，
 * 避免出现「用筛选参数反查他人评价」的越权路径）。</p>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：{@code pageNum} 从 1 开始，{@code pageSize} 最大 100。</p>
 */
@Data
public class EvaluationQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = 10;

    /** 综合评分精确筛选，1~5（如筛选「5 星好评」） */
    private Integer totalScore;

    /**
     * 评分上限筛选，1~5，用于查询低分工单（接口文档 4.7.2 示例：{@code maxScore=2}）。
     * <p>语义为 {@code total_score <= maxScore}，与 {@code totalScore} 精确筛选互不冲突、可叠加。</p>
     */
    private Integer maxScore;

    /** 维修人员 ID，<b>仅管理员可用</b>（非管理员传入返回 403） */
    private Long workerId;

    /** 类目 ID，<b>仅管理员可用</b>（非管理员传入返回 403） */
    private Long categoryId;

    /** 是否已回复筛选：{@code true} 只看已回复，{@code false} 只看未回复，不传则不筛选 */
    private Boolean hasReply;

    /** 评价时间起，格式 yyyy-MM-dd HH:mm:ss（允许只传 yyyy-MM-dd，自动补 00:00:00） */
    private String startTime;

    /** 评价时间止，格式 yyyy-MM-dd HH:mm:ss（允许只传 yyyy-MM-dd，自动补 23:59:59） */
    private String endTime;
}
