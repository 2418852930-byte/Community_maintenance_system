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
 * 报修工单表实体（对应《02-建表SQL（方案A）》第 3 张表 repair_order）。
 *
 * <p>承载模块：<b>模块二 报修工单提交模块</b>（FR-2-01 ~ FR-2-09，接口 O-01 ~ O-06）。
 * 本表是系统的核心业务表，贯穿
 * 「提交 → 审核 → 派单 → 接单 → 维修 → 验收 → 评价」全流程。</p>
 *
 * <p><b>本模块的链路定位</b>：</p>
 * <ul>
 *   <li>上游：承接模块一「报修类目管理」的类目字典，{@code categoryId} 直接关联
 *       {@code repair_category.category_id}，提交前必须校验该类目为「启用」状态（BR-1-05）；</li>
 *   <li>下游：提交后状态固定为 <b>8 待审核</b>，工单流入模块三「工单派单调度」，
 *       由 D-12 审核通过（8 → 0 待派单）后才进入派单链路。</li>
 * </ul>
 *
 * <p><b>字段语义约定（与建表脚本注释逐字对齐）</b>：</p>
 * <ul>
 *   <li>{@code status}：10 态字典，取值与中文名见 {@code OrderStatusEnum}，起始状态恒为 8；</li>
 *   <li>{@code workerId / dispatcherId / dispatchTime / dispatchRemark}：
 *       为「当前有效派单」快照，由<b>下游模块三</b>写入，本模块提交时一律为 {@code null}；</li>
 *   <li>{@code verifyTime / verifyResult / verifyRemark}：
 *       为「最近一次验收」快照，由模块五写入，本模块提交时一律为 {@code null}；</li>
 *   <li>{@code rejectReason / rejectorId / rejectTime}：
 *       为「最近一次审核驳回」快照（V2.0 新增），<b>仅状态 9 已驳回时有值</b>；
 *       提交（O-01）时保持 {@code null}，重新提交（O-06）时统一清空；</li>
 *   <li>{@code cancelReason}：仅状态 8 / 0 / 9 → 7 已撤销时填写（BR-2-07）。</li>
 * </ul>
 *
 * <p><b>方案 A 时间字段配合要求</b>：{@code create_time} 为 TIMESTAMP 自动列，
 * {@code update_time} 为 DATETIME 且默认值固定，须由业务代码显式维护，
 * 因此两者均标注 {@code @TableField(fill = ...)} 交由 {@code MyMetaObjectHandler} 填充。</p>
 */
@Data
@TableName("repair_order")
public class RepairOrder implements Serializable {

    /** 工单ID，主键，自增 */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /** 工单编号：BX + yyyyMMdd + 4 位当日流水号，全局唯一（BR-2-05） */
    private String orderNo;

    /** 报修居民ID，关联 sys_user.user_id（role = resident）；由 JWT 解析，不接受前端传入（BR-2-06） */
    private Long residentId;

    /** 报修类目ID，关联 repair_category.category_id（上游模块一的类目字典） */
    private Long categoryId;

    /** 报修标题，简要描述故障，2~100 字 */
    private String title;

    /** 故障详细描述，10~500 字（BR-2-03） */
    private String description;

    /** 故障现场图片，多张以英文逗号分隔，最多 3 张（FR-2-07），总长 ≤ 1000 */
    private String images;

    /** 上门维修地址，默认取居民 sys_user.address（楼栋房号），可修改（BR-8-05） */
    private String address;

    /** 联系人姓名，2~50 字 */
    private String contactName;

    /** 联系人电话，11 位手机号（BR-2-02） */
    private String contactPhone;

    /** 紧急程度：0-普通，1-紧急；紧急工单在待派单池中置顶展示（FR-2-08） */
    private Integer urgency;

    /** 居民期望上门时间，不得早于当前时间（BR-2-08） */
    private LocalDateTime expectTime;

    /**
     * 工单状态（V2.0，10 态）：
     * 0-待派单，1-已派单，2-已接单，3-维修中，4-待验收，5-已验收，6-已完成，
     * 7-已撤销，<b>8-待审核（起始状态）</b>，9-已驳回。
     */
    private Integer status;

    /** 当前受派维修人员ID（下游模块三写入），关联 sys_user.user_id（role = repair） */
    private Long workerId;

    /** 派单管理员ID（下游模块三写入），关联 sys_user.user_id（role = admin） */
    private Long dispatcherId;

    /** 派单时间（最近一次，下游模块三写入） */
    private LocalDateTime dispatchTime;

    /** 派单备注（最近一次，下游模块三写入） */
    private String dispatchRemark;

    /** 维修人员接单时间（下游模块四写入） */
    private LocalDateTime acceptTime;

    /** 维修完工时间（下游模块四写入） */
    private LocalDateTime finishTime;

    /** 维修费用合计（元），新建工单固定为 0.00 */
    private BigDecimal totalCost;

    /** 验收时间（最近一次快照，模块五写入） */
    private LocalDateTime verifyTime;

    /** 验收结果（最近一次）：0-驳回(返工)，1-通过（模块五写入） */
    private Integer verifyResult;

    /** 验收意见（最近一次，模块五写入） */
    private String verifyRemark;

    /** 【V2.0】管理员审核驳回原因/驳回意见，仅状态 9 时有值，5~200 字（上游驳回、本模块 O-06 清空） */
    private String rejectReason;

    /** 【V2.0】驳回管理员ID，关联 sys_user.user_id（role = admin，最近一次驳回人快照） */
    private Long rejectorId;

    /** 【V2.0】驳回时间（最近一次驳回时间快照） */
    private LocalDateTime rejectTime;

    /** 撤销原因，仅状态 8 待审核 / 0 待派单 / 9 已驳回 → 7 已撤销 时填写（BR-2-07） */
    private String cancelReason;

    /** 提交时间（创建时间），由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除。
     * <p>@JsonIgnore 保证该内部标记不会出现在任何响应中。</p>
     */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
