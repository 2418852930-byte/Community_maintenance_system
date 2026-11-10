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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修记录表实体（对应《02-建表SQL（方案A）》第 4 张表 {@code repair_record}）。
 *
 * <p>本表是「维修进度管理模块（模块四）」的<b>主数据来源</b>，
 * 但<b>模块三的两处接口同样必须写入本表</b>，因此实体在此随模块三一并建出，
 * 模块四可直接复用，<b>不重复定义</b>：</p>
 * <ul>
 *   <li><b>D-08 接单</b>：维修人员确认接单时，自动写入一条 {@code record_type = 1}（接单）
 *       的记录，作为进度时间线的首个节点（接口文档 4.4.8）；</li>
 *   <li><b>D-09 拒单</b>：维修人员拒绝接单时，留存一条 {@code record_type = 6}（转派退回）
 *       的记录，承载拒单原因（接口文档 4.4.9、BR-3-06）。</li>
 * </ul>
 *
 * <p>其余记录类型（2 到达现场 / 3 维修进度 / 4 更换配件 / 5 完工）由模块四写入，
 * 本模块不越界。</p>
 *
 * <p><b>方案 A 时间字段配合要求</b>：{@code create_time} / {@code update_time} 由
 * {@code MyMetaObjectHandler} 填充；{@code record_time} 是<b>业务发生时间</b>，
 * 需由业务代码显式赋值（本模块取当前时间）。</p>
 */
@Data
@TableName("repair_record")
public class RepairRecord implements Serializable {

    /** 记录ID，主键，自增 */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /** 所属工单ID，关联 repair_order.order_id */
    private Long orderId;

    /** 操作人（维修人员）ID，关联 sys_user.user_id（role = repair） */
    private Long workerId;

    /** 记录类型：1-接单，2-到达现场，3-维修进度，4-更换配件，5-完工，6-转派退回 */
    private Integer recordType;

    /** 进度说明文字（NOT NULL），接单时取接单备注，拒单时取拒单原因 */
    private String progressContent;

    /** 现场/完工图片，多张以英文逗号分隔；本模块写入的两类记录不涉及图片，故为 null */
    private String images;

    /** 本次使用/更换的配件；本模块写入的两类记录不涉及配件，故为 null */
    private String partsUsed;

    /** 本次产生的费用（元），本模块写入的两类记录恒为 0.00 */
    private BigDecimal cost;

    /** 记录发生时间（业务时间，由业务代码显式赋值） */
    private LocalDateTime recordTime;

    /** 创建时间，由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除，1-已删除（@JsonIgnore 保证不出现在响应中） */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
