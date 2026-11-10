package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单列表元素 VO（对应接口 O-02「工单分页列表」的 data.list 元素）。
 *
 * <p>依据：接口文档 4.3.2。字段严格对齐文档表格，并额外提供
 * {@code categoryName} / {@code residentName} / {@code workerName} /
 * {@code urgencyName} / {@code statusName} 五个派生字段，
 * 使前端列表<b>无需二次翻译</b>即可直接渲染。</p>
 *
 * <p><b>全流程状态可见</b>：{@code status} / {@code statusName} 覆盖 0~9 全部 10 态。
 * 本模块只写入 8（提交）与 7（撤销），其余状态由下游派单调度 / 维修进度 / 验收 /
 * 评价模块流转；由于列表<b>每次都实时查库</b>，下游任一状态变更都会立刻反映到本页面，
 * 无需任何额外适配（对应任务要求「工单列表可查看全流程状态，与后续模块的状态更新实时联动」）。</p>
 */
@Data
public class OrderListVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 报修标题 */
    private String title;

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称（含父类目，如「水电维修 / 水管漏水」） */
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

    /** 紧急程度 0 普通 / 1 紧急 */
    private Integer urgency;

    /** 紧急程度中文名 */
    private String urgencyName;

    /** 工单状态 0~9 */
    private Integer status;

    /** 状态中文名（含「待审核」「已驳回」） */
    private String statusName;

    /** 维修人员 ID（未派单为 null） */
    private Long workerId;

    /** 维修人员姓名（未派单为 null） */
    private String workerName;

    /** 期望上门时间 */
    private LocalDateTime expectTime;

    /** 维修费用合计 */
    private BigDecimal totalCost;

    /** 提交时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 是否待验收超时（FR-5-06：状态为 4 且完工超过 48 小时） */
    private Boolean verifyOverdue;
}
