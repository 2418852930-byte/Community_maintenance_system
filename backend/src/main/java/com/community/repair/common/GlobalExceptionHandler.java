package com.community.repair.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 *
 * <p>作用：把 Service / Controller 中抛出的异常统一转换为接口文档 2.3 节的 {@link Result} 结构，
 * 并让 HTTP 状态码与业务 code 保持一致（接口文档 2.4 节）。</p>
 *
 * <p>NFR-R-03：不向前端暴露堆栈信息，异常详情只写日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：由 Service 层主动抛出，携带明确的业务状态码与提示语。
     * 例：用户名已存在（409）、原密码错误（409）、管理员禁用本人账号（403）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]：code={}, message={}", request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return build(e.getCode(), e.getMessage(), null);
    }

    /**
     * @Valid 校验失败（@RequestBody 对象参数）。
     * 返回 data 为「字段名 -> 错误提示」的 Map，前端可直接定位到表单项。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            // 同一个字段有多条规则时只保留第一条，提示更简洁
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("参数校验失败：{}", errors);
        return build(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), errors);
    }

    /** 表单/查询参数绑定失败（非 @RequestBody 场景） */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Map<String, String>>> handleBindException(BindException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("参数绑定失败：{}", errors);
        return build(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), errors);
    }

    /** 单个参数约束校验失败（如 @PathVariable / @RequestParam 上的 @Min） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Map<String, String>>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            errors.putIfAbsent(String.valueOf(violation.getPropertyPath()), violation.getMessage());
        }
        return build(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), errors);
    }

    /** 缺少必填的查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return build(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数：" + e.getParameterName(), null);
    }

    /** 请求体不是合法 JSON 或类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return build(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误，请检查 JSON 结构", null);
    }

    /** 请求方法不支持，如用 GET 调用仅支持 PUT 的状态变更接口（接口文档 2.4 节 405） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return build(ResultCode.METHOD_NOT_ALLOWED.getCode(), "请求方法不支持：" + e.getMethod(), null);
    }

    /**
     * 【模块二新增】上传文件超出大小限制（接口文档 2.4 节 413）。
     *
     * <p>触发场景：U-15 上传图片时单张超过 5MB。该限制由容器层
     * （{@code config/FileUploadConfig} 注册的 MultipartConfigElement）先行拦截并抛出
     * {@link MaxUploadSizeExceededException}，若不单独处理会落到兜底分支返回 500，
     * 与接口文档约定的 413 不一致，因此在此显式转换。</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制：{}", e.getMessage());
        return build(ResultCode.PAYLOAD_TOO_LARGE.getCode(), "图片大小不能超过 5MB", null);
    }

    /** 兜底异常：未捕获的所有异常，统一返回 500，不暴露堆栈（NFR-R-03） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return build(ResultCode.ERROR.getCode(), "服务器内部错误，请稍后重试", null);
    }

    /**
     * 构造响应：HTTP 状态码与业务 code 保持一致，便于前端 axios 拦截器统一处理。
     */
    private <T> ResponseEntity<Result<T>> build(Integer code, String message, T data) {
        int safeCode = (code == null) ? ResultCode.ERROR.getCode() : code;
        HttpStatus httpStatus = HttpStatus.resolve(safeCode);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatus).body(Result.build(safeCode, message, data));
    }
}
