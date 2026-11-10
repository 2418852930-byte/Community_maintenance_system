package com.community.repair.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具类（对应 BR-8-07）。
 *
 * <p>令牌载荷包含 {@code userId} 与 {@code role} 两个业务字段，有效期固定 7200 秒（2 小时）。
 * 签名算法 HS256，密钥从 application.yml 的 {@code jwt.secret} 读取（长度需 ≥ 32 字节）。</p>
 *
 * <p>注意：令牌中的 role 只作为前端初始化与快速判断使用，<b>服务端鉴权以数据库最新角色为准</b>
 * （BR-8-17），该逻辑在 {@link JwtInterceptor} 中实现。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    /** 载荷键：用户ID */
    public static final String CLAIM_USER_ID = "userId";

    /** 载荷键：角色编码 */
    public static final String CLAIM_ROLE = "role";

    /** 载荷键：登录账号（便于日志排查） */
    public static final String CLAIM_USERNAME = "username";

    private final SecretKey secretKey;

    /** 令牌有效期（秒），默认 7200 */
    @Getter
    private final long expireSeconds;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-seconds:7200}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /**
     * 签发令牌。
     *
     * @param userId   用户ID
     * @param username 登录账号
     * @param role     角色编码
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析令牌。
     *
     * @param token JWT 字符串
     * @return 载荷 Claims
     * @throws JwtException 令牌被篡改、格式非法或已过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 安全解析：解析失败返回 null，不抛异常。
     *
     * @param token JWT 字符串
     * @return Claims 或 null
     */
    public Claims parseTokenQuietly(String token) {
        try {
            return parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /** 从载荷中取用户ID */
    public Long getUserId(Claims claims) {
        Object value = claims.get(CLAIM_USER_ID);
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    /** 从载荷中取角色编码 */
    public String getRole(Claims claims) {
        Object value = claims.get(CLAIM_ROLE);
        return value == null ? null : String.valueOf(value);
    }

    /** 从载荷中取登录账号 */
    public String getUsername(Claims claims) {
        Object value = claims.get(CLAIM_USERNAME);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 取令牌过期时间戳（毫秒），用于写入黑名单时确定清理时机。
     * 解析失败时按「当前时间 + 有效期」兜底。
     */
    public long getExpireAtMillis(String token) {
        Claims claims = parseTokenQuietly(token);
        if (claims == null || claims.getExpiration() == null) {
            return System.currentTimeMillis() + expireSeconds * 1000;
        }
        return claims.getExpiration().getTime();
    }

    /**
     * 从请求头中剥离前缀，取出纯令牌。
     *
     * @param headerValue 形如 "Bearer eyJhbGciOi..."
     * @param prefix      前缀，默认 "Bearer "
     * @return 纯令牌；格式不符返回 null
     */
    public static String resolveToken(String headerValue, String prefix) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String realPrefix = (prefix == null || prefix.isBlank()) ? "Bearer " : prefix;
        if (headerValue.startsWith(realPrefix)) {
            String token = headerValue.substring(realPrefix.length()).trim();
            return token.isEmpty() ? null : token;
        }
        // 容错：前端直接传了裸 token
        return headerValue.trim();
    }
}
