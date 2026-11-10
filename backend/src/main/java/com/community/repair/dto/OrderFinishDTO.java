package com.community.repair.dto;

import com.community.repair.constant.ProgressConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 提交维修完成请求参数（对应接口 R-04：PUT /api/orders/{orderId}/finish）。
 *
 * <p>依据：接口文档 4.5.4；需求 FR-4-06、BR-4-02、BR-4-04、BR-4-06。</p>
 *
 * <p>维修人员填写维修结果说明并上传维修后照片，工单状态由 <b>3（维修中）置为 4（待验收）</b>，
 * 记录 {@code finish_time}，并生成一条类型为「完工」的维修记录（{@code record_type = 5}）。</p>
 *
 * <p><b>本接口是模块四流向模块五（工单验收管理）的唯一出口</b>：
 * 落库后 {@code repair_order} 具备 {@code status = 4}、{@code finish_time}、{@code total_cost}
 * 三个字段，以及 {@code repair_record} 中的完工节点说明 ——
 * 这正是下游模块五「待验收工单快捷查询（V-01）」与「验收（V-02）」所需的完整输入，
 * <b>无需任何数据结构适配</b>。</p>
 *
 * <p><b>BR-4-06 重复提交</b>：居民验收驳回后工单由 4 回退至 3（由模块五写入），
 * 维修人员可再次调用本接口提交完成，因此本接口<b>不限制调用次数</b>，
 * 只以「当前状态是否为 3」作为唯一前置条件。</p>
 */
@Data
public class OrderFinishDTO implements Serializable {

    /** 维修结果说明，<b>必填</b> 10~300 字（BR-4-04），落库为完工记录的 {@code progress_content} */
    @Size(min = ProgressConstants.FINISH_CONTENT_MIN_LENGTH,
            max = ProgressConstants.FINISH_CONTENT_MAX_LENGTH,
            message = "维修结果说明长度须为 10~300 个字符")
    private String progressContent;

    /** 维修后照片路径，多张以英文逗号分隔，最多 3 张；可为空 */
    @Size(max = ProgressConstants.IMAGES_MAX_LENGTH, message = "维修后照片路径总长度不能超过 1000 个字符")
    private String images;

    /** 使用 / 更换的配件汇总，≤ 255 字；可为空 */
    @Size(max = ProgressConstants.PARTS_USED_MAX_LENGTH, message = "配件汇总长度不能超过 255 个字符")
    private String partsUsed;

    /** 本次完工结算费用（元），默认 0.00，不可为负；会累加至工单 {@code total_cost} */
    @DecimalMin(value = "0.00", message = "本次完工费用不能为负数")
    private BigDecimal cost;
}
