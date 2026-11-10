package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单记录元素 VO（对应接口 D-10 的 data 数组元素）。
 *
 * <p>依据：接口文档 4.4.10；需求 FR-3-07、BR-3-08。</p>
 *
 * <p>数据来源为 {@code repair_dispatch_log} 表，按 {@code dispatch_time} 升序返回，
 * 每次派单（1）/ 改派（2）/ 发布抢单池（3）/ 抢单（4）/ 接单（5）/ 拒单（6）
 * 均独立成行；改派通过 {@code fromWorkerId} / {@code toWorkerId} 成对记录，
 * 可完整还原改派链条。</p>
 *
 * <p>其中 {@code operatorName}、{@code fromWorkerName}、{@code toWorkerName}
 * 为<b>派生字段</b>（联查 {@code sys_user.real_name}），<b>不落库</b>；
 * 由 Service 层批量装配以避免 N+1 查询。</p>
 */
@Data
public class DispatchRecordVO implements Serializable {

    /** 派单记录 ID */
    private Long dispatchId;

    /** 所属工单 ID */
    private Long orderId;

    /** 操作类型：1 派单 / 2 改派 / 3 发布抢单池 / 4 抢单 / 5 接单 / 6 拒单 */
    private Integer dispatchType;

    /** 操作类型中文名 */
    private String dispatchTypeName;

    /** 操作人 ID（派单/改派/发布抢单池为 admin；抢单/接单/拒单为 repair） */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人角色编码：resident / repair / admin */
    private String operatorRole;

    /** 原受派维修人员 ID（仅改派时有值，用于还原改派链条） */
    private Long fromWorkerId;

    /** 原受派维修人员姓名 */
    private String fromWorkerName;

    /** 本次操作涉及的维修人员 ID */
    private Long toWorkerId;

    /** 本次操作涉及的维修人员姓名 */
    private String toWorkerName;

    /** 操作结果：1 成功 / 0 失败 */
    private Integer opResult;

    /** 操作结果中文名：「成功」/「失败」 */
    private String opResultName;

    /** 备注：派单说明 / 改派说明 / 发布说明 / 拒单原因 */
    private String remark;

    /** 操作发生时间 */
    private LocalDateTime dispatchTime;
}
