package com.community.repair.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>Service 层校验不通过时抛出本异常，由 {@link GlobalExceptionHandler} 统一转换为
 * {@link Result} 返回给前端，避免在每个方法里写 if-else 拼装响应。</p>
 *
 * <p>示例：{@code throw new BusinessException(ResultCode.CONFLICT, "用户名或密码错误");}</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
