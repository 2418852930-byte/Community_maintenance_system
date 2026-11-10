package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * S-03 类目分布统计 —— 单个类目的统计行（模块七）。
 *
 * <p>对应接口文档 4.8.3 的 {@code data.list} 元素结构、需求 FR-7-02
 * （按报修类目统计工单数量与占比，以饼图或环形图展示）。</p>
 *
 * <p><b>本对象的双重身份</b>：既作为接口出参，也作为
 * {@code StatisticsMapper#selectCategoryStatLevel1 / Level2} 的聚合行承载对象。
 * Mapper 只负责填 {@code categoryId} / {@code orderCount} / {@code completedCount}，
 * {@code categoryName} 与 {@code ratio} 由 Service 层装配（名称来自模块一的类目全量字典，
 * 占比需要先拿到有效报修量做分母）。</p>
 */
@Data
public class CategoryStatItemVO implements Serializable {

    /**
     * 类目 ID。
     * <p>聚合层级为一级（{@code level=1}）时，该字段是<b>根类目</b>的 ID
     * （SQL 中通过 {@code IF(c.parent_id = 0, c.category_id, c.parent_id)} 归并而成）；
     * 层级为二级（{@code level=2}）时，是工单实际挂载的那个类目 ID。</p>
     */
    private Long categoryId;

    /**
     * 类目名称（由 Service 依据模块一的类目字典装配）。
     * <p>一级聚合时返回根类目名（如「水电维修」）；二级聚合时返回
     * 「父类目 / 子类目」形式（如「水电维修 / 电路故障」），
     * 与模块六 E-02 的 {@code categoryName} 拼装口径完全一致。</p>
     */
    private String categoryName;

    /**
     * 该类目的工单量。
     * <p>口径为<b>有效报修量</b>：含待审核(8)，不含已撤销(7) 与已驳回(9)
     * （接口文档 4.8.3 V2.0 口径说明，对齐 BR-7-04）。</p>
     */
    private Long orderCount;

    /** 该类目中当前状态为「已验收(5) + 已完成(6)」的数量（完成量口径，附录 C2） */
    private Long completedCount;

    /**
     * 占比（单位：%，保留 2 位小数）。
     * <p>计算方式：{@code orderCount ÷ 有效报修量 × 100}。
     * 注意分母是该区间内的<b>有效报修量总和</b>，而不是「本页返回的类目数量之和」，
     * 因此当 {@code topN} 截断了长尾类目时，各项占比之和会小于 100%，属于预期行为
     * （与接口文档 4.8.3 的 {@code total} 字段语义一致）。</p>
     */
    private BigDecimal ratio;
}
