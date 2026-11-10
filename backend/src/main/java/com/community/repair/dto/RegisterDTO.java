package com.community.repair.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 居民账号注册请求参数（对应接口 U-01：POST /api/auth/register）。
 *
 * <p>依据：FR-8-01、BR-8-01 ~ BR-8-06。</p>
 *
 * <p>说明：需求文档 5.8.5 中的「手机号」与「联系电话」在数据库 V2.0 中合并为 phone 单一字段，
 * 注册时二者取值相同，因此本 DTO 只有一个 phone 字段。</p>
 */
@Data
public class RegisterDTO implements Serializable {

    /** 登录账号：4~20 位字母/数字/下划线，全局唯一（BR-8-02） */
    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4~20 位字母、数字或下划线")
    private String username;

    /** 手机号：11 位，全局唯一，作为账号唯一标识（BR-8-01） */
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    /** 登录密码：6~20 位且须同时包含字母与数字（BR-8-03） */
    @NotBlank(message = "请输入密码")
    private String password;

    /** 确认密码：必须与 password 完全一致（BR-8-04），仅用于校验、不入库 */
    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    /** 真实姓名：2~10 个字符 */
    @NotBlank(message = "请输入真实姓名")
    @Size(min = 2, max = 10, message = "真实姓名长度需为 2~10 个字符")
    private String realName;

    /** 楼栋房号：2~50 字，作为报修工单「报修位置」的默认值（BR-8-05） */
    @NotBlank(message = "请输入楼栋房号")
    @Size(min = 2, max = 50, message = "楼栋房号长度需为 2~50 字")
    private String address;

    /** 电子邮箱（选填） */
    @Email(message = "邮箱格式不正确")
    private String email;
}
