package com.community.repair.dto;

import com.community.repair.constant.ProgressConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 开始维修请求参数（对应接口 R-03：PUT /api/orders/{orderId}/start）。
 *
 * <p>依据：接口文档 4.5.3；需求 FR-4-03、BR-4-02。</p>
 *
 * <p>维修人员到达现场后点击「开始维修」，工单状态由 <b>2（已接单）置为 3（维修中）</b>，
 * 并自动生成一条类型为「到达现场」的维修记录（{@code record_type = 2}），
 * 该记录时间同时作为 BR-7-02「维修时长」的起点（完工时间 − 开始维修时间）。</p>
 *
 * <p>状态迁移严格按 BR-4-02 顺序执行，<b>仅状态 2 可调用</b>；
 * 其余状态一律返回 409「请按顺序更新进度」，从机制上禁止跳级
 * （例如「已派单(1)」必须先经 D-08 接单，不可直接开始维修）。</p>
 */
@Data
public class OrderStartDTO implements Serializable {

    /**
     * 到场说明，≤ 200 字，可选。
     * <p>落库为 {@code repair_record.progress_content}；不填时使用默认文案
     * 「维修人员已到达现场，开始维修」（{@link ProgressConstants#DEFAULT_ARRIVE_CONTENT}）。</p>
     */
    @Size(max = ProgressConstants.ARRIVE_REMARK_MAX_LENGTH,
            message = "到场说明长度不能超过 200 个字符")
    private String remark;
}
