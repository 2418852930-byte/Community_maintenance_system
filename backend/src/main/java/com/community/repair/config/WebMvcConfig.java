package com.community.repair.config;

import com.community.repair.constant.FileConstants;
import com.community.repair.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 配置：注册 JWT 鉴权拦截器 + 跨域配置 + 上传图片静态资源映射。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 上传根目录，与 {@code FileController} 使用同一个配置键，保证「写入路径」与「读取路径」一致。
     */
    @Value("${file.upload.root-path:" + FileConstants.DEFAULT_ROOT_PATH + "}")
    private String uploadRootPath;

    /**
     * 注册鉴权拦截器。
     *
     * <p>对应接口文档 2.1 节：U-01 居民账号注册、U-02 账号登录、U-05 忘记密码重置密码
     * 三个接口免鉴权，其余接口全部需要携带 JWT。</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有接口
                .addPathPatterns("/api/**")
                // 放行免鉴权接口与错误页
                .excludePathPatterns(
                        "/api/auth/register",          // U-01 居民账号注册
                        "/api/auth/login",             // U-02 账号登录
                        "/api/auth/forgot-password",   // U-05 忘记密码 / 自助重置密码
                        "/error"
                );
    }

    /**
     * 【模块二新增】上传图片的静态资源映射。
     *
     * <p>U-15 上传接口（{@code FileController}）把图片落盘到 {@code ./uploads/order/yyyyMMdd/}，
     * 并返回形如 {@code /uploads/order/20260918/xxx.jpg} 的相对路径。
     * 若不在 Spring MVC 中显式映射该 URL 前缀，前端 {@code <img src="/uploads/...">}
     * 会因找不到处理器而返回 404。此处把 URL 路径 {@code /uploads/**} 映射到磁盘目录即可。</p>
     *
     * <p>注意两点：</p>
     * <ul>
     *   <li>该映射<b>只读</b>，不提供任何写入入口，写入统一走 U-15 接口并带 JWT 鉴权；</li>
     *   <li>拦截器只拦截 {@code /api/**}，因此 {@code /uploads/**} 天然不需要 JWT，
     *       图片可直接被 {@code <img>} 标签引用（浏览器 img 请求无法携带 Authorization 头）。</li>
     * </ul>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 转成绝对路径并统一为 file: 形式，避免相对路径在不同启动目录下解析不一致
        String absolutePath = Paths.get(uploadRootPath).toAbsolutePath().normalize().toString();
        String location = "file:" + absolutePath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * 跨域配置：开发阶段前端运行在 http://localhost:5173，后端为 8080，需要放开跨域。
     *
     * <p>生产环境建议由 Nginx 反向代理统一域名，或把 allowedOriginPatterns 收窄为实际域名。</p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 允许前端读取响应头（如需自定义头可在此追加）
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
