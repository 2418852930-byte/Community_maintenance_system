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
 * 验收记录表实体（对应《02-建表SQL（方案A）》第 7 张表 {@code repair_verify_log}）。
 *
 * <p>承载模块：<b>模块五 工单验收管理模块</b>（FR-5-01 ~ FR-5-07，接口 V-01 ~ V-04）。</p>
 *
 * <p><b>本表已存在于「方案A」建表脚本中，本模块不新增表、不修改表结构</b>
 * （字段与索引逐字对齐脚本第 313~332 行）。方案 A 兼容 MySQL 5.5~5.6，
 * {@code update_time} 由业务代码（{@code MyMetaObjectHandler}）维护，
 * 因此 {@code createTime} / {@code updateTime} 均标注 {@code @TableField(fill = ...)}。</p>
 *
 * <p><b>为何需要这张表（接口文档 Q4）</b>：
 * {@code repair_order} 上的 {@code verify_time} / {@code verify_result} / {@code verify_remark}
 * 只是「最近一次验收」的<b>快照</b>，无法承载 BR-5-05 要求的
 * 「多次驳回 → 重新维修 → 再次验收，<b>每次验收均独立留痕</b>」。
 * 因此本表采用<b>只追加</b>的留痕设计：每轮验收独立成行，
 * 由 {@code verify_round}（自 1 起递增、与 {@code order_id} 联合唯一）标识轮次，
 * V-04 验收记录查询即直查本表并按 {@code verify_round} 升序返回。</p>
 *
 * <p><b>字段语义约定（与建表脚本注释逐字对齐）</b>：</p>
 * <ul>
 *   <li>{@code verifyType}：1 居民验收 / 2 管理员代验收（BR-5-07 的落库依据）；</li>
 *   <li>{@code verifyResult}：0 驳回 / 1 通过 —— 注意这是<b>验收驳回</b>，
 *       与 {@code repair_order.reject_reason} 的<b>审核驳回</b>是两个不同环节（接口文档 Q9）；</li>
 *   <li>{@code verifyRemark}：验收意见 / 驳回原因 / 代验收说明，驳回与代验收时必填 5~200 字；</li>
 *   <li>{@code verifyTime}：验收动作发生时间（业务时间，由业务代码显式赋值，
 *       因为它同时是 BR-7-02「验收时长」的计算依据）。</li>
 * </ul>
 *
 * <p><b>数据不可篡改</b>：本模块对 {@code repair_verify_log} <b>只做 insert 与 select</b>，
 * 不存在任何 update / delete 路径，验收留痕一经写入即不可修改、不可删除。</p>
 */
@Data
@TableName("repair_verify_log")
public class RepairVerifyLog implements Serializable {

    /** 验收记录ID，主键，自增 */
    @TableId(value = "verify_id", type = IdType.AUTO)
    private Long verifyId;

    /** 所属工单ID，关联 repair_order.order_id */
    private Long orderId;

    /** 验收轮次，自 1 起递增（与 order_id 联合唯一，BR-5-05 多轮留痕的依据） */
    private Integer verifyRound;

    /** 验收人ID，关联 sys_user.user_id（role = resident 或 admin） */
    private Long verifierId;

    /** 验收类型：1-居民验收，2-管理员代验收（对应 VerifyTypeEnum） */
    private Integer verifyType;

    /** 验收结果：0-驳回，1-通过（对应 VerifyResultEnum） */
    private Integer verifyResult;

    /** 验收意见 / 驳回原因 / 代验收说明（驳回与代验收时必填 5~200 字） */
    private String verifyRemark;

    /** 验收时间（业务时间，由业务代码显式赋值；BR-7-02「验收时长」的终点） */
    private LocalDateTime verifyTime;

    /** 创建时间，由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除，1-已删除（留痕表业务上实际不删除，@JsonIgnore 保证不出现在响应中） */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
