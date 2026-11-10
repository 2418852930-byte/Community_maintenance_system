package com.community.repair.controller;

import com.community.repair.common.BusinessException;
import com.community.repair.common.Result;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.FileConstants;
import com.community.repair.security.UserContext;
import com.community.repair.vo.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 公共文件接口 Controller（U-15 上传图片）。
 *
 * <p>对应接口文档 4.1.15；需求 FR-2-07（报修现场照片）、FR-4-05（维修过程照片）、NFR-S-05。</p>
 *
 * <p><b>定位说明</b>：这是<b>公共支撑接口</b>，不属于单一业务模块的功能。
 * 接口文档将其归入「模块零 登录注册与用户管理」，用于头像、报修现场照片、
 * 维修过程照片、评价图片的统一上传。模块一交付说明的待办中已明确「随模块二一并实现」，
 * 因此本模块按文档原样补齐，供后续模块（四、六）直接复用，<b>不重复开发</b>。</p>
 *
 * <p><b>权限</b>：类上不加 {@code @RequireRole}，即「全部登录用户」均可上传
 * （接口文档 4.1.15 权限角色）。鉴权仍由已有的 {@code JwtInterceptor} 统一完成，
 * 未携带合法 JWT 的请求在进入本方法前即返回 401。</p>
 *
 * <p><b>存储策略</b>：本地磁盘存储，按「业务目录 / 日期」分级，
 * 文件名使用 UUID 重命名以避免同名覆盖与中文乱码，落盘路径形如
 * {@code ./uploads/order/20260918/a1b2c3d4e5f6.jpg}，
 * 对外访问路径形如 {@code /uploads/order/20260918/a1b2c3d4e5f6.jpg}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    /** 上传根目录，默认 ./uploads（相对后端运行目录），与静态资源映射保持一致 */
    @Value("${file.upload.root-path:" + FileConstants.DEFAULT_ROOT_PATH + "}")
    private String rootPath;

    /**
     * U-15 上传图片。
     *
     * <p>POST /api/files/upload　全部登录用户　multipart/form-data</p>
     *
     * <p>校验顺序（对应接口文档 4.1.15 的 400 / 415 / 413 三类状态码）：</p>
     * <ol>
     *   <li>未选择文件 → 400；</li>
     *   <li>扩展名或 Content-Type 非 jpg/jpeg/png → 415；</li>
     *   <li>单张超过 5MB → 413（容器层已由 {@code FileUploadConfig} 限制，
     *       此处再做一次业务层兜底，保证无论何种进入路径都能返回文档约定的 413）。</li>
     * </ol>
     *
     * @param file 图片文件
     * @return 图片相对访问路径、原始文件名与文件大小
     */
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        // 前置校验：必须登录（未登录会在拦截器层被拦下，这里兜底保证 Service 语义完整）
        UserContext.require();

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, FileConstants.MSG_FILE_EMPTY);
        }
        if (file.getSize() > FileConstants.MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.PAYLOAD_TOO_LARGE, FileConstants.MSG_FILE_TOO_LARGE);
        }

        // 类型校验：扩展名 + Content-Type 双重判断，防止仅改扩展名的伪装文件
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "image";
        String extension = resolveExtension(originalName);
        String contentType = file.getContentType();
        boolean contentTypeValid = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (extension == null || !FileConstants.ALLOWED_EXTENSIONS.contains(extension) || !contentTypeValid) {
            throw new BusinessException(ResultCode.UNSUPPORTED_MEDIA_TYPE, FileConstants.MSG_FILE_TYPE);
        }

        // 相对目录：order/yyyyMMdd（接口文档示例：/uploads/order/20260917/xxx.jpg）
        String relativeDir = FileConstants.DIR_ORDER + "/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern(FileConstants.DIR_DATE_PATTERN));
        // 文件名用 UUID 重命名：避免同名覆盖、规避中文与特殊字符路径问题
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path targetDir = Paths.get(rootPath, relativeDir).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(storedName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("图片保存失败：dir={}, name={}", targetDir, storedName, e);
            throw new BusinessException(ResultCode.ERROR, FileConstants.MSG_FILE_SAVE_FAIL);
        }

        String url = FileConstants.URL_PREFIX + "/" + relativeDir + "/" + storedName;
        log.info("图片上传成功：user={}, url={}, size={}",
                UserContext.getUserId(), url, file.getSize());

        FileUploadVO vo = new FileUploadVO();
        vo.setUrl(url);
        vo.setFileName(originalName);
        vo.setFileSize(file.getSize());
        return Result.success("上传成功", vo);
    }

    /**
     * 提取小写扩展名；无扩展名时返回 null。
     *
     * @param originalName 原始文件名
     * @return 小写扩展名，如 jpg
     */
    private String resolveExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return null;
        }
        return originalName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
