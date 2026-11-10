package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价详情 VO（对应接口 E-03：GET /api/evaluations/{evaluationId} 的 data 结构）。
 *
 * <p>依据：接口文档 4.7.3 —— 「字段结构同 E-02 列表元素，并附加 {@code order} 子对象
 * （含 {@code orderNo}、{@code title}、{@code categoryName}、{@code finishTime}、
 * {@code totalCost}）」。</p>
 *
 * <p>实现说明：为保持「一个 VO 一处字段定义」的清晰度，本类<b>组合</b>
 * {@link EvaluationVO}（评价主体信息）与 {@link OrderBrief}（工单摘要），
 * 而不是用继承去展开字段 —— 这样 E-02 与 E-03 的字段口径永远同源，
 * 后续 E-02 增删字段时 E-03 自动跟随，不会出现「详情少了列表有的字段」这类漂移。</p>
 *
 * <p>数据范围由服务端强制校验：居民仅可查看本人评价、维修人员仅可查看本人相关评价、
 * 管理员可查看全部；不符返回 403，不存在返回 404。</p>
 */
@Data
public class EvaluationDetailVO implements Serializable {

    /** 评价主体信息（字段与 E-02 列表元素完全一致，含匿名脱敏结果） */
    private EvaluationVO evaluation;

    /** 工单摘要（对应接口文档 4.7.3 要求的 order 子对象） */
    private OrderBrief order;

    /**
     * 工单摘要子对象（接口文档 4.7.3 明确列出的五个字段）。
     *
     * <p>只提供摘要而非完整工单详情，是因为完整工单详情已由模块二 O-03 承载
     * （前端可点击「工单详情」跳转），此处仅用于「评价时回看是哪一单」的场景，
     * 避免在本模块重复实现一套工单详情结构。</p>
     */
    @Data
    public static class OrderBrief implements Serializable {

        /** 工单编号 */
        private String orderNo;

        /** 报修标题 */
        private String title;

        /** 类目名称（含父类目） */
        private String categoryName;

        /** 维修完工时间（BR-7-02「维修时长」的终点） */
        private LocalDateTime finishTime;

        /** 维修费用合计（元） */
        private BigDecimal totalCost;
    }
}
