package com.community.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单记录表实体（对应《02-建表SQL（方案A）》第 6 张表 {@code repair_dispatch_log}）。
 *
 * <p>承载模块：<b>模块三 工单派单调度模块</b>（FR-3-05 ~ FR-3-10，接口 D-02 ~ D-10）。</p>
 *
 * <p><b>本表的定位</b>：需求 FR-3-07 要求「查看某工单的历史派单记录（派单人、被派人、
 * 时间、结果）」，BR-3-08 要求「每次派单、改派、接单、拒单操作均写入派单记录表，
 * 保留完整审计轨迹」。本表即该审计轨迹的<b>唯一数据来源</b>
 * （接口文档 Q3 已确认采用「方案 A：直查 repair_dispatch_log」）。</p>
 *
 * <p><b>与其他表的分工边界</b>：</p>
 * <ul>
 *   <li>{@code repair_order.worker_id / dispatcher_id / dispatch_time / dispatch_remark}
 *       只保存「<b>当前有效派单</b>」快照，历史轮次会被覆盖；</li>
 *   <li>本表是<b>只追加（append-only）</b>的流水表，每次操作独立成行，
 *       改派通过 {@code from_worker_id} / {@code to_worker_id} 成对记录，
 *       从而可完整还原改派链条；</li>
 *   <li>业务上仅「新增」与「查询」，<b>不提供修改 / 删除入口</b>。</li>
 * </ul>
 *
 * <p><b>审核链路不写本表</b>：D-12 工单审核（8 → 0 / 8 → 9）不是派单动作，
 * 依据接口文档 4.4.12 与附录 A「审核链路与派单链路的分界」，
 * 审核环节不产生本表记录。</p>
 *
 * <p><b>方案 A 时间字段配合要求</b>：{@code create_time} 为 TIMESTAMP 自动列，
 * {@code update_time} 为 DATETIME 且默认值固定，须由业务代码显式维护，
 * 因此两者均标注 {@code @TableField(fill = ...)} 交由 {@code MyMetaObjectHandler} 填充。</p>
 */
@Data
@TableName("repair_dispatch_log")
public class RepairDispatchLog implements Serializable {

    /** 派单记录ID，主键，自增 */
    @TableId(value = "dispatch_id", type = IdType.AUTO)
    private Long dispatchId;

    /** 所属工单ID，关联 repair_order.order_id */
    private Long orderId;

    /**
     * 操作类型：1-派单，2-改派，3-发布抢单池，4-抢单，5-接单，6-拒单。
     * <p>取值与中文名统一由 {@code DispatchTypeEnum} 提供，本处只落库编码。</p>
     */
    private Integer dispatchType;

    /**
     * 操作人ID，关联 sys_user.user_id。
     * <p>派单 / 改派 / 发布抢单池为 {@code admin}；抢单 / 接单 / 拒单为 {@code repair}。</p>
     */
    private Long operatorId;

    /** 原受派维修人员ID，关联 sys_user.user_id；<b>仅改派（dispatch_type = 2）时有值</b> */
    private Long fromWorkerId;

    /** 本次操作涉及的维修人员ID，关联 sys_user.user_id（派单/改派/抢单/接单/拒单均有值） */
    private Long toWorkerId;

    /** 操作结果：1-成功，0-失败（本模块只写入成功动作，故实际恒为 1） */
    private Integer opResult;

    /** 备注：派单说明 / 改派说明 / 发布说明 / 拒单原因（拒单原因长度 5~200 字） */
    private String remark;

    /** 操作发生时间（D-10 出参 dispatchTime，按其升序还原轨迹） */
    private LocalDateTime dispatchTime;

    /** 创建时间，由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除。
     * <p>审计表实际不做删除，此处仅与全库口径保持一致；
     * {@code @JsonIgnore} 保证该内部标记不出现在任何响应中。</p>
     */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
