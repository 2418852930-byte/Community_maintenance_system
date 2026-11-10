package com.community.repair.security;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 令牌黑名单（对应 U-03 退出登录）。
 *
 * <p>JWT 是无状态的，服务端无法直接「作废」已签发的令牌。为支持退出登录，
 * 这里把已登出的令牌放入黑名单，并在 {@link JwtInterceptor} 中优先校验。
 * 黑名单条目在令牌自身过期时间到达后自动清理，不会无限增长。</p>
 *
 * <p><b>毕业设计取舍说明</b>：真实项目通常使用 Redis（设置 TTL 让键自动过期，
 * 并支持多实例共享）。本项目为降低环境依赖，采用 JVM 内存实现，
 * 单机部署下功能完全等价；如需升级为 Redis，只需替换本类的存储实现。</p>
 */
@Component
public class TokenBlacklist {

    /** key = token 字符串，value = 该 token 的过期时间戳（毫秒） */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 把令牌加入黑名单。
     *
     * @param token      JWT 令牌
     * @param expireAtMillis 令牌自身的过期时间戳（毫秒），到达后自动清理
     */
    public void add(String token, long expireAtMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        blacklist.put(token, expireAtMillis);
        // 每次写入时做一次惰性清理，避免无序增长
        clearExpired();
    }

    /**
     * 判断令牌是否已在黑名单中。
     *
     * @param token JWT 令牌
     * @return true 表示该令牌已登出，应当拒绝
     */
    public boolean contains(String token) {
        if (token == null) {
            return false;
        }
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        // 已过期的条目视为不存在（令牌本身也已失效）
        if (expireAt <= System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /** 清理所有已过期的黑名单条目 */
    public void clearExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = blacklist.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    /** 当前黑名单条目数（仅供测试观察） */
    public int size() {
        return blacklist.size();
    }
}
