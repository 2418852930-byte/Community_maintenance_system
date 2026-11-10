package com.community.repair.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增用户请求参数（对应接口 U-10：POST /api/users）。
 *
 * <p>依据：FR-8-09、BR-8-15、BR-8-16、BR-8-20。</p>
 *
 * <p><b>重要</b>：本入口不接受 resident 角色（居民只能通过 U-01 自助注册），
 * 提交 role = resident 时 Service 层返回 400。</p>
 */
@Data
public class UserCreateDTO implements Serializable {

    /** 登录账号：4~20 位字母/数字/下划线，全局唯一（BR-8-02） */
    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4~20 位字母、数字或下划线")
    private String username;

    /** 初始密码：6~20 位且含字母与数字（BR-8-03） */
    @NotBlank(message = "请输入初始密码")
    private String password;

    /** 真实姓名：2~10 字 */
    @NotBlank(message = "请输入真实姓名")
    @Size(min = 2, max = 10, message = "真实姓名长度需为 2~10 个字符")
    private String realName;

    /** 角色：repair 维修人员 / admin 系统管理员（不接受 resident，BR-8-16） */
    @NotBlank(message = "请选择角色")
    private String role;

    /** 手机号：11 位，全局唯一（BR-8-01） */
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    /** 性别：0 未知 / 1 男 / 2 女，默认 0 */
    @Min(value = 0, message = "性别取值非法")
    @Max(value = 2, message = "性别取值非法")
    private Integer gender;

    /** 电子邮箱（选填） */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 维修工种（role = repair 时选填），如：水电 / 门窗 / 公共照明 / 给排水 / 综合 */
    @Size(max = 50, message = "维修工种长度不能超过 50 字")
    private String workerType;

    /** 技能描述（role = repair 时选填） */
    @Size(max = 255, message = "技能描述长度不能超过 255 字")
    private String skillDesc;

    /** 工作状态（role = repair 时有效）：0 休息 / 1 空闲 / 2 忙碌，默认 1 空闲 */
    @Min(value = 0, message = "工作状态取值非法")
    @Max(value = 2, message = "工作状态取值非法")
    private Integer workStatus;

    /** 备注（选填） */
    @Size(max = 255, message = "备注长度不能超过 255 字")
    private String remark;
}
