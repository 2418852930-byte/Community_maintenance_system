package com.community.repair.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体（对应接口文档 2.3 节）。
 *
 * <pre>
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": {}
 * }
 * </pre>
 *
 * <p>全项目所有 Controller 统一返回本对象，前端 axios 拦截器只需判断 code 即可。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码，取值见 {@link ResultCode} */
    private Integer code;

    /** 提示信息，可直接用于前端消息提示 */
    private String message;

    /** 业务数据，无数据时为 null */
    private T data;

    private Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功（自定义提示 + 数据） */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /** 失败（默认服务器内部错误） */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /** 失败（指定业务码，对应接口文档 400 / 401 / 403 / 404 / 409 等） */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败（使用状态码枚举自带的默认提示） */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（状态码枚举 + 自定义提示） */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * 任意构造（主要供全局异常处理器使用，可携带错误明细 data）。
     */
    public static <T> Result<T> build(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
