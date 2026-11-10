package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.dto.ResetPasswordDTO;
import com.community.repair.dto.RoleUpdateDTO;
import com.community.repair.dto.StatusUpdateDTO;
import com.community.repair.dto.UserCreateDTO;
import com.community.repair.dto.UserQueryDTO;
import com.community.repair.dto.UserUpdateDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.SysUserService;
import com.community.repair.vo.RoleChangeVO;
import com.community.repair.vo.StatusChangeVO;
import com.community.repair.vo.UserDetailVO;
import com.community.repair.vo.UserListVO;
import com.community.repair.vo.UserSimpleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理类接口 Controller（U-08 ~ U-14）。
 *
 * <p>对应需求 FR-8-07 ~ FR-8-12，接口文档 4.1.8 ~ 4.1.14 节。</p>
 *
 * <p>类上标注 {@code @RequireRole(RoleEnum.ADMIN)}，由 {@link com.community.repair.security.JwtInterceptor}
 * 统一做服务端角色校验：非管理员访问返回 403，且不返回任何数据。</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class UserController {

    private final SysUserService sysUserService;

    /**
     * U-08 用户分页列表。
     *
     * <p>GET /api/users　仅系统管理员</p>
     *
     * <p>支持 keyword（用户名/姓名/手机号）模糊查询，role、status、workerType、
     * workStatus、startTime、endTime 组合筛选。</p>
     */
    @GetMapping
    public Result<PageResult<UserListVO>> pageUsers(UserQueryDTO query) {
        return Result.success("查询成功", sysUserService.pageUsers(query));
    }

    /**
     * U-09 用户详情。
     *
     * <p>GET /api/users/{userId}　仅系统管理员</p>
     */
    @GetMapping("/{userId}")
    public Result<UserDetailVO> getUserDetail(@PathVariable("userId") Long userId) {
        return Result.success("查询成功", sysUserService.getUserDetail(userId));
    }

    /**
     * U-10 新增用户（维修人员 / 系统管理员）。
     *
     * <p>POST /api/users　仅系统管理员</p>
     *
     * <p>依据 BR-8-16，本入口不接受 resident 角色。</p>
     */
    @PostMapping
    public Result<UserSimpleVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success("新增成功", sysUserService.createUser(dto));
    }

    /**
     * U-11 修改用户基本信息。
     *
     * <p>PUT /api/users/{userId}　仅系统管理员</p>
     *
     * <p>注册手机号与创建时间不可修改（BR-8-14）。</p>
     */
    @PutMapping("/{userId}")
    public Result<UserDetailVO> updateUser(@PathVariable("userId") Long userId,
                                           @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success("保存成功", sysUserService.updateUser(userId, dto));
    }

    /**
     * U-12 角色分配与变更。
     *
     * <p>PUT /api/users/{userId}/role　仅系统管理员</p>
     *
     * <p>不可变更本人账号角色，且须保留至少一个启用的管理员（BR-8-18）。</p>
     */
    @PutMapping("/{userId}/role")
    public Result<RoleChangeVO> changeRole(@PathVariable("userId") Long userId,
                                           @Valid @RequestBody RoleUpdateDTO dto) {
        return Result.success("角色变更成功", sysUserService.changeRole(userId, dto));
    }

    /**
     * U-13 账号启用 / 禁用。
     *
     * <p>PUT /api/users/{userId}/status　仅系统管理员</p>
     *
     * <p>不可禁用本人账号，且须保留至少一个启用的管理员（BR-8-18）。</p>
     */
    @PutMapping("/{userId}/status")
    public Result<StatusChangeVO> changeStatus(@PathVariable("userId") Long userId,
                                               @Valid @RequestBody StatusUpdateDTO dto) {
        return Result.success("操作成功", sysUserService.changeStatus(userId, dto));
    }

    /**
     * U-14 重置用户密码。
     *
     * <p>PUT /api/users/{userId}/reset-password　仅系统管理员</p>
     *
     * <p>新密码须满足强度规则，重置成功后清零该账号的登录失败次数（BR-8-21）。</p>
     */
    @PutMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable("userId") Long userId,
                                      @Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(userId, dto);
        return Result.success("密码重置成功", null);
    }
}
