package com.community.repair.util;

import com.community.repair.constant.UserConstants;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类（对应 BR-8-22：用户密码一律采用 BCrypt 加密存储）。
 *
 * <p>BCrypt 特点：</p>
 * <ul>
 *   <li>密文固定 60 位、以 {@code $2a$} 开头，与建表 SQL 中 sys_user.password 的 VARCHAR(100) 匹配；</li>
 *   <li>每次加密都会随机生成盐值，同一明文两次加密结果不同，密文不可逆；</li>
 *   <li>校验使用 {@link BCryptPasswordEncoder#matches(CharSequence, String)}，内部自动提取盐值比对。</li>
 * </ul>
 */
public final class PasswordUtil {

    /** BCryptPasswordEncoder 线程安全，做成静态常量复用即可 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * 加密明文密码。
     *
     * @param rawPassword 明文密码（如 123456）
     * @return BCrypt 密文（60 位）
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文密码与密文是否匹配。
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 数据库中存放的 BCrypt 密文
     * @return true 表示密码正确
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 校验密码强度：6~20 位且同时包含字母与数字（BR-8-03）。
     *
     * @param rawPassword 明文密码
     * @return true 表示符合强度要求
     */
    public static boolean isValidStrength(String rawPassword) {
        return rawPassword != null && rawPassword.matches(UserConstants.REGEX_PASSWORD);
    }
}
