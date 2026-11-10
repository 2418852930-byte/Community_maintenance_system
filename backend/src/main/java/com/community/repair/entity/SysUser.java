package com.community.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表实体（对应《02-建表SQL（方案A）》第 1 张表 sys_user）。
 *
 * <p>承载模块：登录注册与用户管理模块（FR-8-01 ~ FR-8-12）。</p>
 *
 * <p>关键约定：</p>
 * <ul>
 *   <li>role：字符编码 {@code resident} 居民 / {@code repair} 维修人员 / {@code admin} 系统管理员；</li>
 *   <li>status：0 禁用 / 1 启用；</li>
 *   <li>password：BCrypt 密文，长度 60，<b>任何接口都不得返回</b>（BR-8-22），
 *       因此这里加 {@code @JsonIgnore} 做双重保险；</li>
 *   <li>deleted：逻辑删除标记，用户表被多表引用，只允许逻辑删除。</li>
 * </ul>
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    /** 用户ID，主键，自增 */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /** 登录账号，4~20位字母/数字/下划线，全局唯一（BR-8-02） */
    private String username;

    /**
     * 登录密码，BCrypt 密文（BR-8-22）。
     * <p>@JsonIgnore 保证即使误把实体直接返回，密码也不会被序列化到响应中。</p>
     */
    @JsonIgnore
    private String password;

    /** 真实姓名，2~10个字符 */
    private String realName;

    /** 角色编码：resident / repair / admin */
    private String role;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 手机号，11位，全局唯一，账号唯一标识（BR-8-01） */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 居住地址/楼栋房号，仅居民使用，作为报修工单「报修位置」默认值（BR-8-05） */
    private String address;

    /** 头像图片相对路径 */
    private String avatar;

    /** 维修工种，仅维修人员使用 */
    private String workerType;

    /** 技能描述，仅维修人员使用 */
    private String skillDesc;

    /** 工作状态：0-休息，1-空闲，2-忙碌，仅维修人员使用 */
    private Integer workStatus;

    /** 账号状态：0-禁用，1-启用（BR-8-09、FR-8-12） */
    private Integer status;

    /** 最后登录时间，登录成功后更新；从未登录为 null（FR-8-07、FR-8-08） */
    private LocalDateTime lastLoginTime;

    /** 连续登录失败次数，登录成功或重置密码后清零；累计达 5 次触发锁定（BR-8-08） */
    private Integer loginFailCount;

    /**
     * 账号锁定截止时间，null 或早于当前时间表示未锁定（BR-8-08）。
     *
     * <p>需要把该列显式置为 NULL 的场景（登录成功、重置密码后解锁），
     * 在 Service 中使用 LambdaUpdateWrapper 的 set 方法实现，本字段保持默认策略。</p>
     */
    private LocalDateTime lockUntil;

    /** 备注 */
    private String remark;

    /** 创建时间，由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
