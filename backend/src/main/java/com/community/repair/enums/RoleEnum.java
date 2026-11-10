package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 角色枚举（对应接口文档 2.6 节「数据字典（1）用户角色 role」）。
 *
 * <p>数据库 sys_user.role 为 VARCHAR(20) 字符编码，取值与需求文档 5.8.2.3 的
 * 角色编码字典一一对应（RESIDENT / REPAIRMAN / ADMIN）。</p>
 */
@Getter
public enum RoleEnum {

    /** 居民：报修发起方与验收方，唯一可自助注册的角色（BR-8-06） */
    RESIDENT("resident", "居民"),

    /** 维修人员：维修执行方，账号由管理员创建 */
    REPAIR("repair", "维修人员"),

    /** 系统管理员：平台运营与调度方 */
    ADMIN("admin", "系统管理员");

    /** 角色编码（入库值、JWT 载荷值、前端路由 meta 值） */
    private final String code;

    /** 角色中文名（接口出参 roleName） */
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据角色编码获取枚举；编码非法时返回 null。
     *
     * @param code 角色编码，如 "resident"
     * @return 对应枚举，不存在返回 null
     */
    public static RoleEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 角色编码 -> 中文名；编码非法时原样返回，保证前端不会显示 null。
     *
     * @param code 角色编码
     * @return 角色中文名
     */
    public static String nameOf(String code) {
        RoleEnum roleEnum = ofCode(code);
        return roleEnum == null ? code : roleEnum.getName();
    }

    /**
     * 判断角色编码是否合法。
     *
     * @param code 角色编码
     * @return true 表示是 resident / repair / admin 之一
     */
    public static boolean isValid(String code) {
        return ofCode(code) != null;
    }
}
