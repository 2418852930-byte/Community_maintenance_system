package com.community.repair.common;

import lombok.Getter;

/**
 * 业务状态码字典（对应接口文档 2.4 节「状态码规范」）。
 *
 * <p>注意：HTTP 状态码与响应体 code 保持一致，便于前端统一拦截处理。</p>
 */
@Getter
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 请求参数错误：必填项缺失、格式错误、密码强度不足、字段长度超限等 */
    BAD_REQUEST(400, "参数校验失败"),

    /** 未登录或登录已失效：未携带 Token、Token 过期/被篡改、账号已被禁用 */
    UNAUTHORIZED(401, "未登录或登录已失效"),

    /** 权限不足：角色不符、访问他人数据、管理员禁用本人账号 */
    FORBIDDEN(403, "权限不足"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 业务状态冲突：用户名/手机号重复、账号锁定、原密码错误等 */
    CONFLICT(409, "业务状态冲突"),

    /** 文件过大 */
    PAYLOAD_TOO_LARGE(413, "文件过大"),

    /** 文件类型不支持 */
    UNSUPPORTED_MEDIA_TYPE(415, "文件类型不支持"),

    /** 服务器内部错误 */
    ERROR(500, "服务器内部错误");

    /** 业务状态码 */
    private final Integer code;

    /** 默认提示信息 */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
