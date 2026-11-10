package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验收通过 / 驳回结果 VO（对应接口 V-02 的 data 结构）。
 *
 * <p>依据：接口文档 4.6.2 的「返回参数（data）」表格与响应示例，字段逐一对齐。</p>
 *
 * <p><b>本 VO 是模块五交付给模块六（服务评价管理）的「评价入口契约」</b>：
 * {@code canEvaluate = true} 即明确告知前端「该工单已满足提交评价的前置条件」，
 * 与模块六 FR-6-01「待评价工单列表 = O-02 + status=5」的口径一致 ——
 * 也就是说，模块六既可以通过本字段做即时引导，也可以通过列表接口事后补漏，
 * 两条路径读到的都是同一份 {@code repair_order.status = 5}，<b>无需任何适配</b>。</p>
 */
@Data
public class VerifyResultVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 变更后状态：通过为 5（已验收），驳回为 3（维修中） */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 验收结果：0 驳回 / 1 通过（原样回显，便于前端确认本次动作） */
    private Integer verifyResult;

    /** 验收结果中文名 */
    private String verifyResultName;

    /** 本轮验收轮次（写入 repair_verify_log.verify_round，自 1 起递增） */
    private Integer verifyRound;

    /**
     * 验收时间。
     *
     * <p><b>驳回时为 null</b>（接口文档 4.6.2 明确说明）。原因是「验收时间」在业务上
     * 指<b>验收完成</b>的时间点，而驳回并未使工单进入「已验收」状态，
     * 因此 {@code repair_order.verify_time} 在驳回时不被写入。
     * 这与 BR-7-02「验收时长 = 验收时间 − 完工时间」的口径互为因果：
     * 若驳回也写 verify_time，返修中的工单就会被算出一个虚假的验收时长。</p>
     */
    private LocalDateTime verifyTime;

    /** 验收意见（通过时为可选的补充说明，驳回时为驳回原因） */
    private String verifyRemark;

    /** 是否解锁评价入口：通过时为 true（FR-5-03），驳回时为 false */
    private Boolean canEvaluate;
}
