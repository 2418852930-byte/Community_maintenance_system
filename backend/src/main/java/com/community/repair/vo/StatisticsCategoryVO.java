package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * S-03 类目分布统计 响应对象（模块七）。
 *
 * <p>对应接口文档 4.8.3 的 {@code data} 结构、需求 FR-7-02。
 * 前端以 ECharts <b>环形饼图</b>渲染 {@code list} 的 {@code categoryName -> orderCount} 分布。</p>
 */
@Data
public class StatisticsCategoryVO implements Serializable {

    /**
     * 参与统计的工单总数（<b>有效报修量</b>口径：含待审核(8)，不含已撤销(7) 与已驳回(9)）。
     * <p>该值同时充当各 {@code ratio} 的统一分母。</p>
     */
    private Long total;

    /**
     * 类目统计数据，按 {@code orderCount} <b>降序</b>排列（饼图的图例顺序即为占比由大到小）。
     * <p>最多返回 {@code topN} 条（默认 10），区间内无数据时为空数组。</p>
     */
    private List<CategoryStatItemVO> list = new ArrayList<>();
}
