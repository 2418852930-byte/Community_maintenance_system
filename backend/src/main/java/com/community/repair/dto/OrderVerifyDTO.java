package com.community.repair.dto;

import com.community.repair.constant.VerifyConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 验收通过 / 驳回请求参数（对应接口 V-02：PUT /api/orders/{orderId}/verify）。
 *
 * <p>依据：接口文档 4.6.2；需求 FR-5-03、FR-5-04、BR-5-01 ~ BR-5-06。</p>
 *
 * <p><b>同一入参承载两种结果</b>（接口文档把这组动作设计为一个接口）：</p>
 * <ul>
 *   <li>{@code verifyResult = 1}（通过）：工单状态 <b>4 → 5（已验收）</b>，
 *       写入 {@code verify_time} / {@code verify_result} / {@code verify_remark}，
 *       并解锁该工单的服务评价入口（返回 {@code canEvaluate = true}）；</li>
 *   <li>{@code verifyResult = 0}（驳回）：工单状态 <b>4 → 3（维修中）</b>，
 *       原受派维修人员不变（BR-5-04），工单重新流入模块四返修，
 *       此时 {@code verifyRemark} <b>必填</b>（BR-5-03）。</li>
 * </ul>
 *
 * <p>无论通过还是驳回，每次操作都向 {@code repair_verify_log} 写入一行独立记录，
 * {@code verify_round} 自 1 起递增，{@code verify_type = 1}（居民验收）。</p>
 *
 * <p><b>参数校验分两层</b>：{@code @NotNull} / {@code @Min} / {@code @Max} 由
 * {@code @Valid} 完成第一层（结果取值域、长度上限）；
 * 「驳回时意见必填」与「非空格校验」由 Service 层完成第二层，
 * 因为「是否必填」依赖另一个字段的取值，是 Bean Validation 单字段注解无法表达的业务规则。</p>
 */
@Data
public class OrderVerifyDTO implements Serializable {

    /**
     * 验收结果：0 驳回（返工） / 1 通过，<b>必填</b>。
     *
     * <p>越界时由 {@code @Min/@Max} 拦截并返回 400，
     * Service 层再用 {@code VerifyResultEnum.isValid} 兜底一次，
     * 保证「不合法的结果值绝不会进入状态机」。</p>
     */
    @NotNull(message = "验收结果不能为空")
    @Min(value = 0, message = "验收结果只能是 0（驳回返工）或 1（通过）")
    @Max(value = 1, message = "验收结果只能是 0（驳回返工）或 1（通过）")
    private Integer verifyResult;

    /**
     * 验收意见（驳回时即驳回原因），<b>驳回时必填</b>，5~200 字。
     *
     * <p>通过时可留空（表示「无补充意见」）。长度上限在此处声明为 200，
     * 与业务收紧口径一致（数据库列宽 VARCHAR(500) 留有余量）。</p>
     */
    @Size(max = VerifyConstants.VERIFY_REMARK_MAX_LENGTH, message = "验收意见长度不能超过 200 个字符")
    private String verifyRemark;
}
