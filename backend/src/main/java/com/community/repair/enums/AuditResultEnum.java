package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 工单审核结果枚举（对应接口文档 4.4.12「D-12 工单审核」的 {@code auditResult}）。
 *
 * <p>本枚举是 V2.0 新增的「审核链路」专属字典，用于把 D-12 的一次调用
 * 分派为两条互斥的状态迁移路径：</p>
 * <pre>
 * | 值 | 名称     | 状态迁移 | 附带写入                                        |
 * |----|----------|----------|-------------------------------------------------|
 * | 1  | 审核通过 | 8 → 0    | 无（reject_* 三字段保持 NULL）                   |
 * | 0  | 审核驳回 | 8 → 9    | reject_reason / rejector_id / reject_time（必填） |
 * </pre>
 *
 * <p><b>与「验收驳回」的区别</b>（接口文档 Q9，语义刻意区分）：</p>
 * <ul>
 *   <li>本枚举的「审核驳回」是<b>管理员对居民报修内容的合规性审核</b>，
 *       落库字段 {@code repair_order.reject_reason}，状态 8 → 9；</li>
 *   <li>「验收驳回」是<b>居民对维修结果的验收</b>，落库字段
 *       {@code repair_verify_log.verify_result = 0}，状态 4 → 3，
 *       由模块五实现，与本模块无关。</li>
 * </ul>
 */
@Getter
public enum AuditResultEnum {

    /** 1 审核通过：状态 8 → 0，工单进入待派单池 */
    PASS(1, "审核通过"),

    /** 0 审核驳回：状态 8 → 9，必须填写驳回原因（BR-3-09） */
    REJECT(0, "审核驳回");

    /** 审核结果编码（入参 auditResult / 出参 auditResult） */
    private final Integer code;

    /** 审核结果中文名（出参 auditResultName） */
    private final String name;

    AuditResultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取枚举；编码非法时返回 null。
     *
     * @param code 审核结果编码，1 通过 / 0 驳回
     * @return 对应枚举，不存在返回 null
     */
    public static AuditResultEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 编码 -> 中文名；编码非法时返回「未知」。
     *
     * @param code 审核结果编码
     * @return 审核结果中文名
     */
    public static String nameOf(Integer code) {
        AuditResultEnum resultEnum = ofCode(code);
        return resultEnum == null ? "未知" : resultEnum.getName();
    }

    /**
     * 判断编码是否合法（只能是 1 通过 或 0 驳回）。
     *
     * @param code 审核结果编码
     * @return true 表示合法
     */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }
}
