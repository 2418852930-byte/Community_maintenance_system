package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交服务评价结果 VO（对应接口 E-01 的 data 结构）。
 *
 * <p>依据：接口文档 4.7.1 的「返回参数（data）」表格与响应示例，字段逐一对齐。</p>
 *
 * <p><b>本 VO 是模块六交付给前端的「业务闭环凭证」</b>：{@code orderStatus = 6（已完成）}
 * 表示该工单已走完「提交 → 审核 → 派单 → 接单 → 维修 → 验收 → 评价」全链路，
 * 业务闭环结束（BR-6-04）。前端据此把该工单从「待评价」列表中移除。</p>
 */
@Data
public class EvaluationCreateVO implements Serializable {

    /** 评价 ID（repair_evaluation.evaluation_id） */
    private Long evaluationId;

    /** 工单 ID */
    private Long orderId;

    /** 工单编号（BX + yyyyMMdd + 4 位流水，便于前端直接回显） */
    private String orderNo;

    /** 服务态度评分，1~5 */
    private Integer scoreService;

    /** 响应速度评分，1~5 */
    private Integer scoreSpeed;

    /** 维修质量评分，1~5 */
    private Integer scoreQuality;

    /** 综合评分（三项平均值四舍五入），1~5 */
    private Integer totalScore;

    /** 工单变更后状态，固定 6（已完成） */
    private Integer orderStatus;

    /** 工单状态中文名，固定「已完成」 */
    private String orderStatusName;

    /** 评价时间 */
    private LocalDateTime createTime;
}
