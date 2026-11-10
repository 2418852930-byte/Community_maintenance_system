package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 验收结果枚举（模块五：工单验收管理）。
 *
 * <p>对应接口文档 2.6 节「数据字典（6）验收结果 verifyResult」、
 * 数据库 V2.0 第 7 张表 {@code repair_verify_log.verify_result} 的列注释，
 * 以及 {@code repair_order.verify_result}（最近一次验收结果快照）。</p>
 *
 * <pre>
 * | 值 | 名称 | 说明                     | 状态流转（BR-5-01）          |
 * | 0  | 驳回 | 维修不合格，退回返修     | 4 待验收 → 3 维修中（FR-5-04）|
 * | 1  | 通过 | 维修合格，验收通过       | 4 待验收 → 5 已验收（FR-5-03）|
 * </pre>
 *
 * <p><b>与「审核驳回」的严格区分（接口文档 Q9）</b>：
 * 本枚举表达的是<b>验收环节</b>的驳回（{@code verify_result}），
 * 与管理员<b>审核环节</b>的驳回（{@code repair_order.reject_reason} / {@code rejector_id} /
 * {@code reject_time}）<strong>在字段、参数名与状态机三个层面均刻意区分</strong>，
 * 二者绝不可混用。验收驳回回到 3（维修中）；审核驳回进入 9（已驳回）。</p>
 *
 * <p><b>与 {@code AuditResultEnum} 的关系</b>：{@code AuditResultEnum} 服务于模块三的
 * 管理员审核（D-12），取值范围与业务语义不同，本枚举<b>不复用、不覆盖</b>它。</p>
 */
@Getter
public enum VerifyResultEnum {

    /** 0 驳回：维修不合格，工单退回「维修中(3)」，由原受派维修人员返修（FR-5-04） */
    REJECT(0, "驳回"),

    /** 1 通过：维修合格，工单流转为「已验收(5)」，解锁模块六评价入口（FR-5-03） */
    PASS(1, "通过");

    /** 验收结果码（入库值，对应 verify_result） */
    private final Integer code;

    /** 验收结果中文名（接口出参 verifyResultName，前端标签直接展示） */
    private final String name;

    VerifyResultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据验收结果码获取枚举；非 0~1 的取值返回 null。
     *
     * @param code 验收结果码
     * @return 对应枚举，不存在返回 null
     */
    public static VerifyResultEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 验收结果码 -> 中文名；取值非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 验收结果码
     * @return 验收结果中文名
     */
    public static String nameOf(Integer code) {
        VerifyResultEnum resultEnum = ofCode(code);
        return resultEnum == null ? "未知" : resultEnum.getName();
    }

    /**
     * 判断取值是否属于合法的验收结果（0 或 1）。
     *
     * <p>用途：V-02 入参的第一道取值域校验，越界即 400
     * 「验收结果只能是 0（驳回返工）或 1（通过）」。</p>
     *
     * @param code 验收结果码
     * @return true 表示合法
     */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }

    /**
     * 判断本结果是否为「通过」（用于决定目标状态与 {@code canEvaluate}）。
     *
     * @param code 验收结果码
     * @return true 表示通过
     */
    public static boolean isPass(Integer code) {
        return PASS.getCode().equals(code);
    }
}
