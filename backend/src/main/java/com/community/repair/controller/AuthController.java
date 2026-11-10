package com.community.repair.controller;

import com.community.repair.common.Result;
import com.community.repair.dto.ChangePasswordDTO;
import com.community.repair.dto.ForgotPasswordDTO;
import com.community.repair.dto.LoginDTO;
import com.community.repair.dto.RegisterDTO;
import com.community.repair.dto.UpdateProfileDTO;
import com.community.repair.security.JwtUtil;
import com.community.repair.service.SysUserService;
import com.community.repair.vo.LoginVO;
import com.community.repair.vo.UserInfoVO;
import com.community.repair.vo.UserSimpleVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证类接口 Controller（U-01 ~ U-07）。
 *
 * <p>对应需求 FR-8-01 ~ FR-8-06，接口文档 4.1.1 ~ 4.1.7 节。</p>
 *
 * <p>免鉴权接口：U-01 注册、U-02 登录、U-05 忘记密码（在 WebMvcConfig 中放行）。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.prefix:Bearer }")
    private String tokenPrefix;

    /**
     * U-01 居民账号注册。
     *
     * <p>POST /api/auth/register　免鉴权</p>
     *
     * <p>角色固定为 resident，注册成功后不自动登录、不签发 JWT，前端跳转登录页。</p>
     */
    @PostMapping("/register")
    public Result<UserSimpleVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserSimpleVO vo = sysUserService.register(dto);
        return Result.success("注册成功，请登录", vo);
    }

    /**
     * U-02 账号登录。
     *
     * <p>POST /api/auth/login　免鉴权</p>
     *
     * <p>支持用户名或手机号登录，成功后返回 JWT 与用户信息。</p>
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = sysUserService.login(dto);
        return Result.success("登录成功", vo);
    }

    /**
     * U-03 退出登录。
     *
     * <p>POST /api/auth/logout　全部登录用户</p>
     *
     * <p>服务端把当前令牌加入黑名单至其自然过期，前端同时清除本地令牌。</p>
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = JwtUtil.resolveToken(request.getHeader(tokenHeader), tokenPrefix);
        sysUserService.logout(token);
        return Result.success("退出成功", null);
    }

    /**
     * U-04 获取当前登录用户信息。
     *
     * <p>GET /api/auth/me　全部登录用户</p>
     *
     * <p>用于前端顶栏展示、权限初始化与个人信息表单回显；不返回 password。</p>
     */
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser() {
        return Result.success("查询成功", sysUserService.getCurrentUser());
    }

    /**
     * U-05 忘记密码 / 自助重置密码。
     *
     * <p>POST /api/auth/forgot-password　免鉴权</p>
     *
     * <p>须同时校验用户名与注册预留手机号，二者匹配方可重置。</p>
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        sysUserService.forgotPassword(dto);
        return Result.success("密码重置成功，请使用新密码登录", null);
    }

    /**
     * U-06 修改本人密码。
     *
     * <p>PUT /api/auth/password　全部登录用户</p>
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        sysUserService.changePassword(dto);
        return Result.success("密码修改成功", null);
    }

    /**
     * U-07 修改本人资料。
     *
     * <p>PUT /api/auth/me　全部登录用户</p>
     *
     * <p>手机号作为账号唯一标识不允许自行修改。</p>
     */
    @PutMapping("/me")
    public Result<UserInfoVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success("保存成功", sysUserService.updateProfile(dto));
    }
}
