package com.community.repair.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户上下文对象。
 *
 * <p>由 {@link JwtInterceptor} 在每次请求通过鉴权后写入 {@link UserContext}，
 * Service / Controller 层通过 {@code UserContext.get()} 获取，避免层层传参。</p>
 *
 * <p>注意：这里保存的是<b>从数据库实时查出的最新角色</b>，而不是 JWT 令牌里的旧角色，
 * 以满足 BR-8-17「角色变更在下一次请求即生效」。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 实时角色编码：resident / repair / admin */
    private String role;
}
