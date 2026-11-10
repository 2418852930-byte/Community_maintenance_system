package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单详情 VO（对应接口 O-03：GET /api/orders/{orderId} 的 data 结构）。
 *
 * <p>依据：接口文档 4.3.3；需求 FR-2-05、FR-5-02。</p>
 *
 * <p><b>三处 V2.0 要点</b>：</p>
 * <ol>
 *   <li><b>审核驳回四要素</b>：{@code rejectReason} / {@code rejectorId} /
 *       {@code rejectorName} / {@code rejectTime}，<b>仅状态 9 已驳回时有值</b>，
 *       其余状态一律为 {@code null}（依据 FR-2-05，详情页据此展示驳回提示区）。
 *       其中 {@code rejectorName} 为派生字段，由 {@code rejectorId} 联查
 *       {@code sys_user.real_name} 得到，<b>不落库</b>；</li>
 *   <li><b>canOperate</b>：当前登录用户可执行的操作标识集合，见 {@link OrderOperateVO}；</li>
 *   <li><b>evaluated</b>：是否已评价，前端据此控制评价入口显隐（一单一评，BR-6-02）。</li>
 * </ol>
 *
 * <p>派单类字段（{@code workerId} / {@code dispatcherId} / {@code dispatchTime} /
 * {@code dispatchRemark}）与验收类字段（{@code verifyTime} / {@code verifyResult} /
 * {@code verifyRemark}）在本模块提交时均为 {@code null}，由下游模块回填——
 * 详情页对 null 做「-」展示即可，不影响流程。</p>
 */
@Data
public class OrderDetailVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 报修标题 */
    private String title;

    /** 故障详细描述 */
    private String description;

    /** 现场照片路径，逗号分隔 */
    private String images;

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称（含父类目） */
    private String categoryName;

    /** 报修居民 ID */
    private Long residentId;

    /** 报修居民姓名 */
    private String residentName;

    /** 上门维修地址 */
    private String address;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 紧急程度 */
    private Integer urgency;

    /** 紧急程度中文名 */
    private String urgencyName;

    /** 期望上门时间 */
    private LocalDateTime expectTime;

    /** 工单状态 0~9 */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 审核驳回原因/驳回意见，仅状态 9 时有值，其余为 null */
    private String rejectReason;

    /** 驳回管理员 ID，仅状态 9 时有值 */
    private Long rejectorId;

    /** 驳回管理员姓名（联查 sys_user.real_name 派生，非落库字段） */
    private String rejectorName;

    /** 驳回时间，仅状态 9 时有值 */
    private LocalDateTime rejectTime;

    /** 维修人员 ID */
    private Long workerId;

    /** 维修人员姓名 */
    private String workerName;

    /** 派单管理员 ID */
    private Long dispatcherId;

    /** 派单管理员姓名 */
    private String dispatcherName;

    /** 派单时间 */
    private LocalDateTime dispatchTime;

    /** 派单备注 */
    private String dispatchRemark;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 完工时间 */
    private LocalDateTime finishTime;

    /** 维修费用合计 */
    private BigDecimal totalCost;

    /** 验收时间 */
    private LocalDateTime verifyTime;

    /** 验收结果 0 不通过 / 1 通过 */
    private Integer verifyResult;

    /** 验收意见 */
    private String verifyRemark;

    /** 撤销原因 */
    private String cancelReason;

    /** 提交时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 是否待验收超时 */
    private Boolean verifyOverdue;

    /** 是否已评价（用于前端控制评价入口显隐） */
    private Boolean evaluated;

    /** 当前登录用户可执行的操作标识，见 {@link OrderOperateVO} */
    private OrderOperateVO canOperate;
}
