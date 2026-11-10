package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修人员负载视图查询条件（对应接口 D-01：GET /api/workers/workload）。
 *
 * <p>依据：接口文档 4.4.1；需求 FR-3-06、数据库 V2.0 附录 C2。</p>
 *
 * <p>本接口返回全部维修人员及其工单量指标，用于辅助管理员均衡派单，
 * 也是前端「派单弹窗」选择维修人员时的数据来源。</p>
 */
@Data
public class WorkerWorkloadQueryDTO implements Serializable {

    /** 按维修工种筛选（对应 sys_user.worker_type），为空表示不限 */
    private String workerType;

    /**
     * 是否仅返回账号正常的维修人员（{@code sys_user.status = 1}）。
     * <p>默认 {@code true}：派单场景下停用账号不可被选择（BR-3-03），
     * 传 {@code false} 可查看含停用人员在内的完整负载分布。</p>
     */
    private Boolean onlyEnabled = Boolean.TRUE;
}
