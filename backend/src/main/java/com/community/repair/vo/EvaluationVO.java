package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价列表元素 VO（对应接口 E-02「评价分页列表」的 data.list 元素）。
 *
 * <p>依据：接口文档 4.7.2。字段严格对齐文档表格，并额外提供
 * {@code categoryName} / {@code residentName} / {@code workerName} 三个派生字段，
 * 使前端列表<b>无需二次翻译</b>即可直接渲染（名称由服务端批量装配，避免 N+1 查询）。</p>
 *
 * <p><b>匿名脱敏（BR-6-06）</b>：当 {@code isAnonymous = 1} 且查看者不是评价人本人时，
 * 服务端会把 {@code residentId} 置为 {@code null}、{@code residentName} 置为「匿名用户」，
 * 使维修人员与管理员都看不到评价人的隐私信息。评价人本人回看自己的评价时不做脱敏。</p>
 *
 * <p><b>下游模块对接</b>：本 VO 同时是模块七「报修统计分析模块」获取评价明细的推荐出口，
 * 其中 {@code totalScore} 对应数据库 {@code idx_worker_score(worker_id, total_score)}
 * 索引，可支撑「维修人员评价均分」等统计（S-07）。</p>
 */
@Data
public class EvaluationVO implements Serializable {

    /** 评价 ID */
    private Long evaluationId;

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 类目名称（含父类目，如「水电维修 / 水管漏水」） */
    private String categoryName;

    /** 评价居民 ID（匿名时对非本人返回 null） */
    private Long residentId;

    /** 评价居民姓名（匿名时对非本人返回「匿名用户」） */
    private String residentName;

    /** 被评价维修人员 ID */
    private Long workerId;

    /** 被评价维修人员姓名 */
    private String workerName;

    /** 服务态度评分，1~5 */
    private Integer scoreService;

    /** 响应速度评分，1~5 */
    private Integer scoreSpeed;

    /** 维修质量评分，1~5 */
    private Integer scoreQuality;

    /** 综合评分，1~5（三项均值四舍五入） */
    private Integer totalScore;

    /** 评价内容 */
    private String content;

    /** 评价图片路径（多张以英文逗号分隔） */
    private String images;

    /** 是否匿名：0 实名 / 1 匿名 */
    private Integer isAnonymous;

    /** 管理员回复内容（未回复为 null） */
    private String replyContent;

    /** 回复时间（未回复为 null） */
    private LocalDateTime replyTime;

    /** 评价时间 */
    private LocalDateTime createTime;
}
