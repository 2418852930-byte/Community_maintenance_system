package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * S-04 工单状态分布统计 响应对象（模块七）。
 *
 * <p>对应接口文档 4.8.4 的 {@code data} 结构、需求 FR-7-03。
 * 前端以饼图 / 表格渲染 {@code list}；状态项<b>按 0 ~ 9 升序完整输出</b>，
 * 无数据的状态项也会出现且 {@code orderCount} 为 0（接口文档 4.8.4 状态码表明确要求），
 * 因此前端无需自行补齐缺失状态。</p>
 */
@Data
public class StatisticsStatusVO implements Serializable {

    /** 有效报修量（不含已撤销 7 与已驳回 9，含待审核 8）—— 同时充当各 {@code ratio} 的统一分母 */
    private Long total;

    /** 10 态完整分布（按状态码升序，含计数为 0 的状态项） */
    private List<StatusStatItemVO> list = new ArrayList<>();
}
