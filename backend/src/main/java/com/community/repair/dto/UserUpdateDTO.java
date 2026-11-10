package com.community.repair.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改用户基本信息请求参数（对应接口 U-11：PUT /api/users/{userId}）。
 *
 * <p>依据：FR-8-10、BR-8-14、BR-8-20。</p>
 *
 * <p><b>关键约束</b>：注册手机号 phone（唯一标识）与 create_time 不可修改，
 * 请求体中出现这些字段一律返回 400。username 亦不支持修改，密码修改请使用 U-14。</p>
 */
@Data
public class UserUpdateDTO implements Serializable {

    /** 真实姓名 */
    @Size(min = 2, max = 10, message = "真实姓名长度需为 2~10 个字符")
    private String realName;

    /** 性别：0 未知 / 1 男 / 2 女 */
    @Min(value = 0, message = "性别取值非法")
    @Max(value = 2, message = "性别取值非法")
    private Integer gender;

    /** 电子邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 楼栋房号 */
    @Size(max = 255, message = "楼栋房号长度不能超过 255 字")
    private String address;

    /** 维修工种 */
    @Size(max = 50, message = "维修工种长度不能超过 50 字")
    private String workerType;

    /** 技能描述 */
    @Size(max = 255, message = "技能描述长度不能超过 255 字")
    private String skillDesc;

    /** 工作状态：0 休息 / 1 空闲 / 2 忙碌 */
    @Min(value = 0, message = "工作状态取值非法")
    @Max(value = 2, message = "工作状态取值非法")
    private Integer workStatus;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255 字")
    private String remark;

    // ========================================================================
    // 只读哨兵字段：不接受修改，提交即返回 400（BR-8-14）
    // ========================================================================

    /** 【只读哨兵】注册手机号，提交即报错 */
    private String phone;

    /** 【只读哨兵】登录账号，提交即报错 */
    private String username;

    /** 【只读哨兵】创建时间，提交即报错（字符串形式，仅用于识别前端误传） */
    private String createTime;
}
