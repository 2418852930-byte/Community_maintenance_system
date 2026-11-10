package com.community.repair.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改本人资料请求参数（对应接口 U-07：PUT /api/auth/me）。
 *
 * <p>依据：FR-8-06、BR-8-13。</p>
 *
 * <p>不可修改字段：username、role、status 一律忽略；
 * <b>phone（注册手机号）作为账号唯一标识不允许自行修改</b>，
 * 若请求体中提交了 phone 字段则返回 400 并提示「账号手机号不可自行修改，请联系管理员」。</p>
 */
@Data
public class UpdateProfileDTO implements Serializable {

    /** 真实姓名：2~10 字（选填，传了才校验） */
    @Size(min = 2, max = 10, message = "真实姓名长度需为 2~10 个字符")
    private String realName;

    /** 性别：0 未知 / 1 男 / 2 女 */
    @Min(value = 0, message = "性别取值非法")
    @Max(value = 2, message = "性别取值非法")
    private Integer gender;

    /** 电子邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 楼栋房号（仅居民可提交），长度 ≤ 255 */
    @Size(max = 255, message = "楼栋房号长度不能超过 255 字")
    private String address;

    /** 头像路径（由图片上传接口返回，本模块暂不实现上传接口） */
    @Size(max = 255, message = "头像路径过长")
    private String avatar;

    /** 维修工种（仅维修人员可提交） */
    @Size(max = 50, message = "维修工种长度不能超过 50 字")
    private String workerType;

    /** 技能描述（仅维修人员可提交） */
    @Size(max = 255, message = "技能描述长度不能超过 255 字")
    private String skillDesc;

    /** 工作状态：0 休息 / 1 空闲 / 2 忙碌（仅维修人员可提交） */
    @Min(value = 0, message = "工作状态取值非法")
    @Max(value = 2, message = "工作状态取值非法")
    private Integer workStatus;

    // ========================================================================
    // 以下字段是「防误改哨兵」：接口不接受修改，一旦前端传了值，Service 层会返回 400。
    // 目的：即使前端表单被篡改，也无法绕过 BR-8-13「手机号不可自行修改」的限制。
    // ========================================================================

    /** 【只读哨兵】手机号，提交即报错 */
    private String phone;

    /** 【只读哨兵】用户名，提交即报错 */
    private String username;

    /** 【只读哨兵】角色，提交即报错 */
    private String role;

    /** 【只读哨兵】账号状态，提交即报错 */
    private Integer status;
}
