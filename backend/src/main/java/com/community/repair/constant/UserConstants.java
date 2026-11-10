package com.community.repair.constant;

/**
 * 用户模块业务常量。
 *
 * <p>把需求文档中出现的魔法数字集中定义，便于论文说明与后续维护。</p>
 */
public final class UserConstants {

    private UserConstants() {
    }

    // ------------------------- 账号状态（sys_user.status） -------------------------

    /** 账号状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 账号状态：启用 */
    public static final int STATUS_ENABLED = 1;

    // ------------------------- 登录失败锁定（BR-8-08） -------------------------

    /** 触发锁定所需的连续登录失败次数 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 账号锁定持续分钟数 */
    public static final int LOCK_MINUTES = 10;

    // ------------------------- 校验正则 -------------------------

    /** 用户名：4~20 位字母、数字或下划线（BR-8-02） */
    public static final String REGEX_USERNAME = "^[A-Za-z0-9_]{4,20}$";

    /** 手机号：11 位，1 开头，第二位 3~9（BR-8-01） */
    public static final String REGEX_PHONE = "^1[3-9]\\d{9}$";

    /**
     * 密码：6~20 位，必须同时包含字母与数字（BR-8-03）。
     * <p>用两个零宽正向断言分别要求「至少一个字母」与「至少一个数字」。</p>
     */
    public static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[\\S]{6,20}$";

    /** 密码强度校验失败时的统一提示 */
    public static final String MSG_PASSWORD_RULE = "密码需为 6~20 位且同时包含字母和数字";

    // ------------------------- 统一提示语（对齐需求文档 5.8.7 异常处理） -------------------------

    /** 用户名或密码错误（不区分具体错误项，避免账号枚举） */
    public static final String MSG_LOGIN_FAIL = "用户名或密码错误";

    /** 账号被禁用 */
    public static final String MSG_ACCOUNT_DISABLED = "账号已被禁用，请联系管理员";

    /** 账号锁定中 */
    public static final String MSG_ACCOUNT_LOCKED = "登录失败次数过多，请 10 分钟后再试";

    /** 用户名已存在 */
    public static final String MSG_USERNAME_EXISTS = "该用户名已存在，请更换后重试";

    /** 手机号已注册 */
    public static final String MSG_PHONE_EXISTS = "该手机号已注册，请直接登录或找回密码";

    /** 找回密码时用户名与预留手机号不匹配 */
    public static final String MSG_FORGOT_NOT_MATCH = "用户名与预留手机号不匹配";

    /** 手机号不可自行修改 */
    public static final String MSG_PHONE_READONLY = "账号手机号不可自行修改，请联系管理员";

    /** 不可禁用当前登录的管理员账号 */
    public static final String MSG_CANNOT_DISABLE_SELF = "不能禁用当前登录的管理员账号";

    /** 不可变更本人账号的角色 */
    public static final String MSG_CANNOT_CHANGE_SELF_ROLE = "不能变更当前登录管理员账号的角色";

    /** 须保留至少一个启用的管理员账号 */
    public static final String MSG_KEEP_LAST_ADMIN = "系统须至少保留一个启用的管理员账号";
}
