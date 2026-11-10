package com.community.repair.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量派单结果 VO（对应接口 D-03 的 data 结构）。
 *
 * <p>依据：接口文档 4.4.3；需求 FR-3-03、NFR-R-01。</p>
 *
 * <p><b>处理策略</b>：整批在<b>单一事务</b>中逐单执行派单，单条工单的
 * 「工单状态更新 + 派单记录写入」保持原子；
 * 状态不合法的工单<b>不阻断</b>其余工单，而是记录到 {@link FailItem} 明细中返回，
 * 因此接口可能返回「部分成功」的结果（与接口文档 4.4.3 的响应示例一致：
 * {@code successCount = 3}、{@code failCount = 1}）。</p>
 */
@Data
public class BatchDispatchVO implements Serializable {

    /** 派单成功条数 */
    private Integer successCount = 0;

    /** 派单失败条数 */
    private Integer failCount = 0;

    /** 失败明细，元素含 orderId、orderNo、reason；全部成功时为空数组 */
    private List<FailItem> failList = new ArrayList<>();

    /**
     * 单条失败明细。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailItem implements Serializable {

        /** 失败的工单 ID；工单不存在时为请求传入的原值 */
        private Long orderId;

        /** 工单编号；工单不存在时为 null */
        private String orderNo;

        /** 失败原因，如「该工单尚未通过审核，请先审核」 */
        private String reason;
    }
}
