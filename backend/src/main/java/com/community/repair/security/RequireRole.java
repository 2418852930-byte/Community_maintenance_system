package com.community.repair.security;

import com.community.repair.enums.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色权限注解（对应接口文档 2.2 节「鉴权与角色约定」）。
 *
 * <p>标注在 Controller 类或方法上，由 {@link JwtInterceptor} 统一校验；
 * 未标注则表示「任意登录用户均可访问」。</p>
 *
 * <p>用法示例：</p>
 * <pre>
 *   // 仅管理员可访问（U-08 ~ U-14 用户管理接口）
 *   &#64;RequireRole(RoleEnum.ADMIN)
 *   public Result&lt;PageResult&lt;UserListVO&gt;&gt; pageUsers(...) { ... }
 *
 *   // 管理员与维修人员均可访问（后续业务模块使用）
 *   &#64;RequireRole({RoleEnum.ADMIN, RoleEnum.REPAIR})
 * </pre>
 *
 * <p>方法上的注解优先于类上的注解。校验不通过时统一返回 403。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色集合，取值为 {@link RoleEnum}。
     * 留空表示不限制（等价于「任意登录用户」）。
     */
    RoleEnum[] value() default {};
}
