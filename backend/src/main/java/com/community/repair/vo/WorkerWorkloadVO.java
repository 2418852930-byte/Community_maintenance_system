package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修人员负载视图元素 VO（对应接口 D-01 的 data 数组元素）。
 *
 * <p>依据：接口文档 4.4.1；需求 FR-3-06、数据库 V2.0 附录 C2。</p>
 *
 * <p>本视图用于辅助管理员<b>均衡派单</b>，也是前端「派单弹窗」在选择维修人员时
 * 展示在途工单量的数据来源。</p>
 *
 * <p><b>统计口径（严格对齐数据库 V2.0 附录 C2）</b>：</p>
 * <pre>
 * | 指标             | 口径                        | 说明                        |
 * |------------------|-----------------------------|-----------------------------|
 * | pendingCount     | 状态 1 / 2 / 3 / 4 的工单数 | 在途（已派单~待验收）        |
 * | processingCount  | 状态 2 / 3 的工单数         | 处理中（已接单~维修中）      |
 * | finishedCount    | 状态 5 / 6 的工单数         | 累计完成（已验收~已完成）    |
 * </pre>
 * <p><b>待审核(8) / 已驳回(9) / 已撤销(7) 未进入派单链路，不计入任何工作量指标。</b></p>
 */
@Data
public class WorkerWorkloadVO implements Serializable {

    /** 维修人员 ID */
    private Long workerId;

    /** 姓名 */
    private String realName;

    /** 联系电话 */
    private String phone;

    /** 维修工种，如「水暖工」 */
    private String workerType;

    /** 技能描述 */
    private String skillDesc;

    /** 工作状态：0 休息 / 1 空闲 / 2 忙碌（sys_user.work_status） */
    private Integer workStatus;

    /** 账号状态：0 禁用 / 1 启用（sys_user.status）；为 0 时不可被派单（BR-3-03） */
    private Integer status;

    /** 在途工单数（状态 1~4） */
    private Long pendingCount;

    /** 处理中工单数（状态 2~3） */
    private Long processingCount;

    /** 累计完成工单数（状态 5~6） */
    private Long finishedCount;
}
