package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修人员工单量聚合结果（<b>内部查询结果对象</b>，不作为接口出参直接对外）。
 *
 * <p>用途：承载 {@code RepairOrderStatMapper#selectWorkerWorkloadStat} 的
 * {@code GROUP BY worker_id} 聚合结果，一张 SQL 即算出每位维修人员的
 * 在途 / 处理中 / 累计完成三个指标，避免「先查全部工单再在内存里分组」的低效做法。</p>
 *
 * <p>Service 层拿到本对象后，再批量补齐用户档案字段
 * （姓名 / 电话 / 工种 / 技能 / 工作状态 / 账号状态），组装为对外的
 * {@link WorkerWorkloadVO}。这样拆分的目的是让「聚合」与「档案装配」两个关注点分离，
 * 同时保持 SQL 只扫一张表。</p>
 */
@Data
public class WorkerWorkloadStatVO implements Serializable {

    /** 维修人员 ID（repair_order.worker_id） */
    private Long workerId;

    /** 在途工单数（状态 1~4） */
    private Long pendingCount;

    /** 处理中工单数（状态 2~3） */
    private Long processingCount;

    /** 累计完成工单数（状态 5~6） */
    private Long finishedCount;
}
