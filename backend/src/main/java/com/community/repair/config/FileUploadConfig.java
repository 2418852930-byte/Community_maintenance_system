package com.community.repair.config;

import com.community.repair.constant.FileConstants;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传大小限制配置（为公共接口 U-15 提供支撑）。
 *
 * <p><b>为什么需要这个配置类？</b>Spring Boot 对 multipart 请求的默认上限是
 * <b>单文件 1MB</b>，而接口文档 4.1.15 要求支持「单张 ≤ 5MB」。
 * 若不放开该限制，用户在报修页上传稍大的现场照片时，
 * 请求会在进入 Controller 之前就被容器直接拒绝，表现为 500 而不是文档约定的 413。</p>
 *
 * <p><b>实现方式的选择</b>：Spring Boot 的 {@link MultipartAutoConfiguration}
 * 带有 {@code @ConditionalOnMissingBean(MultipartConfigElement.class)} 条件，
 * 因此这里<b>只需显式声明一个 {@link MultipartConfigElement} Bean</b>，
 * 即可在<b>完全不修改 application.yml</b> 的前提下改写上传上限。</p>
 *
 * <p>这样做的好处是：本模块的整合交付不必触碰你本地已经配置好的
 * {@code application.yml}（其中含数据库账号密码等个性化设置），
 * 避免覆盖造成「解压后连不上数据库」的问题。</p>
 *
 * <p><b>如需调整</b>：直接修改 {@link FileConstants#MAX_FILE_SIZE} 与
 * {@link FileConstants#MAX_REQUEST_SIZE} 两个常量即可。</p>
 */
@Configuration
public class FileUploadConfig {

    /**
     * 自定义 multipart 限制（直接使用 Servlet 标准构造器，不依赖 Spring Boot 内部工厂类）：
     * <ul>
     *   <li>{@code location = null}：使用容器默认临时目录</li>
     *   <li>单文件上限：5MB（NFR-S-05、接口文档 4.1.15）</li>
     *   <li>整请求上限：15MB（报修最多 3 张照片，预留余量）</li>
     *   <li>写入磁盘阈值：0 —— 不做内存中转，直接落盘，避免大图占用堆内存</li>
     * </ul>
     *
     * @return multipart 配置
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
                null,
                FileConstants.MAX_FILE_SIZE,
                FileConstants.MAX_REQUEST_SIZE,
                0);
    }
}
