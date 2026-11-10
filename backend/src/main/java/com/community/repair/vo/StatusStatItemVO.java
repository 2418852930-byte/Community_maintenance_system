package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * S-04 工单状态分布统计 —— 单个状态行（模块七）。
 *
 * <p>对应接口文档 4.8.4 的 {@code data.list} 元素结构、需求 FR-7-03
 * （统计各状态的工单数量与占比）。</p>
 *
 * <p><b>本对象的双重身份</b>：既作为接口出参，也作为
 * {@code StatisticsMapper#selectStatusDistribution} 的聚合行承载对象。
 * Mapper 只填 {@code status} / {@code orderCount}，
 * {@code statusName} 与 {@code ratio} 由 Service 层装配——
 * 状态中文名统一取自模块二的 {@code OrderStatusEnum}，
 * <b>本模块不重复维护状态字典</b>。</p>
 */
@Data
public class StatusStatItemVO implements Serializable {

    /** 状态码（0 ~ 9，10 态字典，取值见 {@code OrderStatusEnum}） */
    private Integer status;

    /** 状态中文名（如「待审核」「维修中」），由 {@code OrderStatusEnum.nameOf} 提供 */
    private String statusName;

    /** 该状态的工单数量（10 态完整输出，无数据的项为 0） */
    private Long orderCount;

    /**
     * 占比（单位：%，保留 2 位小数）。
     * <p><b>分母统一为「有效报修量」</b>（不含已撤销 7 与已驳回 9），
     * 即使是「已撤销」「已驳回」两项也仍以有效报修量为分母，
     * 这样做的好处是「能直观看出无效工单占有效工单的比例」，
     * 与接口文档 4.8.4 响应示例的算法一致（示例中 status=7 的 9 条按 259 计算得 3.47%）。</p>
     */
    private BigDecimal ratio;
}
