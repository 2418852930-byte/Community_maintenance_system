package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 紧急程度枚举（对应接口文档 2.6 节「数据字典（3）紧急程度 urgency」）。
 *
 * <p>需求文档附录 B 与《待确认事项汇总》Q2 已确认采用 <b>2 级</b>口径：</p>
 * <pre>
 * | 值 | 名称 | 说明                                       |
 * | 0  | 普通 | 默认级别，按提交时间顺序派单               |
 * | 1  | 紧急 | 待派单池中置顶展示，优先派单（FR-2-08）    |
 * </pre>
 *
 * <p>对应落库字段 {@code repair_order.urgency TINYINT NOT NULL DEFAULT 0}。</p>
 */
@Getter
public enum UrgencyEnum {

    /** 0 普通：默认级别，按提交时间顺序派单 */
    NORMAL(0, "普通"),

    /** 1 紧急：待派单池中置顶展示，优先派单 */
    URGENT(1, "紧急");

    /** 紧急程度编码（入库值，对应 repair_order.urgency） */
    private final Integer code;

    /** 紧急程度中文名（接口出参 urgencyName） */
    private final String name;

    UrgencyEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取枚举；编码非法时返回 null。
     *
     * @param code 编码 0 / 1
     * @return 对应枚举，不存在返回 null
     */
    public static UrgencyEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 编码 -> 中文名；编码非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 编码
     * @return 紧急程度中文名
     */
    public static String nameOf(Integer code) {
        UrgencyEnum urgencyEnum = ofCode(code);
        return urgencyEnum == null ? "未知" : urgencyEnum.getName();
    }

    /**
     * 判断编码是否合法（只能是 0 普通 或 1 紧急）。
     *
     * @param code 编码
     * @return true 表示合法
     */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }
}
