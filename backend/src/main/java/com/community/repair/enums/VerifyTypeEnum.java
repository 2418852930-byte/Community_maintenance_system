package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 验收类型枚举（模块五：工单验收管理）。
 *
 * <p>对应接口文档 2.6 节「数据字典（5）验收类型 verifyType」、
 * 数据库 V2.0 第 7 张表 {@code repair_verify_log.verify_type} 的列注释，
 * 以及 BR-5-07「管理员代验收必须标识操作类型并填写说明」。</p>
 *
 * <pre>
 * | 值 | 名称       | 说明                                             | 写入方                |
 * | 1  | 居民验收   | 工单提交者本人对维修结果确认或驳回（FR-5-03/04） | 模块五 V-02           |
 * | 2  | 管理员代验收 | 待验收超时后管理员代为验收（FR-5-07）          | 模块五 V-03           |
 * </pre>
 *
 * <p><b>为什么要单独建一个类型枚举，而不是复用角色？</b>
 * 因为「谁操作的」与「以什么身份操作的」在业务上并不等价：
 * 管理员既可能是「被居民委托代验收」（type = 2），
 * 未来也可能以其它身份写入验收记录。把类型独立成枚举，
 * V-04 出参的 {@code verifyTypeName} 与统计口径都只依赖本枚举一处定义。</p>
 *
 * <p><b>与 {@code RoleEnum} 的关系</b>：{@code repair_verify_log.verifier_id} 指向的用户
 * 角色只可能是 {@code resident}（居民验收）或 {@code admin}（管理员代验收），
 * 本枚举<b>只描述验收动作的发起方式</b>，不重复定义角色。</p>
 */
@Getter
public enum VerifyTypeEnum {

    /** 1 居民验收：工单提交者本人在「我的验收」页面确认或驳回（V-02） */
    RESIDENT(1, "居民验收"),

    /** 2 管理员代验收：待验收超时后由管理员代为验收，仅支持通过（V-03，BR-5-07） */
    ADMIN_AGENT(2, "管理员代验收");

    /** 验收类型码（入库值，对应 repair_verify_log.verify_type） */
    private final Integer code;

    /** 验收类型中文名（接口出参 verifyTypeName，前端标签直接展示） */
    private final String name;

    VerifyTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据验收类型码获取枚举；非 1~2 的取值返回 null。
     *
     * @param code 验收类型码
     * @return 对应枚举，不存在返回 null
     */
    public static VerifyTypeEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 验收类型码 -> 中文名；取值非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 验收类型码
     * @return 验收类型中文名
     */
    public static String nameOf(Integer code) {
        VerifyTypeEnum typeEnum = ofCode(code);
        return typeEnum == null ? "未知" : typeEnum.getName();
    }
}
