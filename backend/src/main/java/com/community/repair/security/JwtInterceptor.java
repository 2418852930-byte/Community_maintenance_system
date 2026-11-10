package com.community.repair.security;

import com.community.repair.common.Result;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.UserConstants;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * JWT 登录鉴权拦截器（对应接口文档 2.2 节与需求文档 5.8.2.4「权限控制实现要点」）。
 *
 * <p>执行顺序（任一步不通过即中断请求并返回错误）：</p>
 * <ol>
 *   <li><b>令牌有效性</b>：请求头是否携带 JWT、是否在黑名单（已登出）、签名是否合法、是否过期 → 401；</li>
 *   <li><b>用户存在性</b>：按令牌中的 userId 回查数据库 → 401；</li>
 *   <li><b>账号状态实时性</b>：status = 0 立即拒绝 → 401「账号已被禁用，请联系管理员」（BR-8-09）；</li>
 *   <li><b>角色实时性</b>：使用<b>数据库中最新角色</b>而非令牌内的旧角色（BR-8-17）；</li>
 *   <li><b>角色权限</b>：校验 {@link RequireRole} 声明的可访问角色 → 403；</li>
 *   <li>写入 {@link UserContext}，供后续 Service / Controller 使用。</li>
 * </ol>
 *
 * <p>说明：前端隐藏菜单只是用户体验优化，不能作为权限依据（NFR-S-03），
 * 因此这里做的是服务端强制校验。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 非 Controller 方法（如静态资源、错误页转发）不做鉴权
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // ---------- ① 令牌有效性 ----------
        String token = JwtUtil.resolveToken(request.getHeader(tokenHeader), tokenPrefix);
        if (token == null) {
            return writeError(response, ResultCode.UNAUTHORIZED, "未登录或登录已失效，请重新登录");
        }
        if (tokenBlacklist.contains(token)) {
            return writeError(response, ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        var claims = jwtUtil.parseTokenQuietly(token);
        if (claims == null) {
            return writeError(response, ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        // ---------- ② 用户存在性 + ③ 账号状态实时性 ----------
        Long userId = jwtUtil.getUserId(claims);
        if (userId == null) {
            return writeError(response, ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return writeError(response, ResultCode.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != UserConstants.STATUS_ENABLED) {
            // 账号禁用即时生效：不依赖客户端登出动作，被禁用用户已有令牌在下次请求即被拒绝
            return writeError(response, ResultCode.UNAUTHORIZED, UserConstants.MSG_ACCOUNT_DISABLED);
        }

        // ---------- ④ 使用数据库中的最新角色（角色变更实时生效） ----------
        String currentRole = user.getRole();

        // ---------- ⑤ 角色权限校验 ----------
        RequireRole requireRole = resolveRequireRole(handlerMethod);
        if (requireRole != null && requireRole.value().length > 0) {
            boolean allowed = Arrays.stream(requireRole.value())
                    .anyMatch(role -> role.getCode().equalsIgnoreCase(currentRole));
            if (!allowed) {
                log.warn("鉴权失败：用户 {}（角色 {}）访问受限接口 {} {}", user.getUsername(), currentRole,
                        request.getMethod(), request.getRequestURI());
                return writeError(response, ResultCode.FORBIDDEN, "无操作权限");
            }
        }

        // ---------- ⑥ 写入线程上下文 ----------
        UserContext.set(new LoginUser(user.getUserId(), user.getUsername(), user.getRealName(), currentRole));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理 ThreadLocal，防止 Tomcat 线程复用造成用户身份串号
        UserContext.clear();
    }

    /**
     * 解析目标方法上的角色要求：方法级注解优先于类级注解。
     */
    private RequireRole resolveRequireRole(HandlerMethod handlerMethod) {
        RequireRole methodAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireRole.class);
    }

    /**
     * 输出统一错误响应（HTTP 状态码与业务 code 保持一致）。
     */
    private boolean writeError(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setStatus(resultCode.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(resultCode.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
        return false;
    }
}
