package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * S-02 报修量趋势统计 响应对象（模块七）。
 *
 * <p>对应接口文档 4.8.2 的 {@code data} 结构、需求 FR-7-01。
 * 前端以 ECharts <b>折线图</b>渲染 {@code list} 的 {@code timeLabel -> orderCount} 曲线。</p>
 */
@Data
public class StatisticsTrendVO implements Serializable {

    /**
     * 实际生效的统计粒度（回显）。
     * <p>取值 {@code day} / {@code week} / {@code month}。前端据此决定 X 轴的格式化方式，
     * 避免前端「请求了 month 却按 day 格式显示」。未传 {@code granularity} 时后端按 {@code day} 处理并回显。</p>
     */
    private String granularity;

    /** 区间内工单总数（= 各时间点 {@code orderCount} 之和，含全部 10 种状态） */
    private Long total;

    /**
     * 时间序列。
     * <p>按 {@code timeLabel} <b>升序</b>排列，保证折线图 X 轴方向正确。
     * 区间内无任何工单时为空数组（接口文档 4.8.2 状态码表：「区间内无数据时 list 为空数组、total 为 0」）。</p>
     *
     * <p><b>连续性问题</b>：为让折线图不出现断点，Service 层会在 {@code list} 非空时
     * 按所选粒度<b>补全区间内的全部时间点</b>（缺失点各计数填 0）；
     * 只有当区间内确实一条工单都没有时，才返回空数组，与接口文档的定义保持一致。</p>
     */
    private List<TrendItemVO> list = new ArrayList<>();
}
