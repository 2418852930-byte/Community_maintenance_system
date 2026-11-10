package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.ChangePasswordDTO;
import com.community.repair.dto.ForgotPasswordDTO;
import com.community.repair.dto.LoginDTO;
import com.community.repair.dto.RegisterDTO;
import com.community.repair.dto.ResetPasswordDTO;
import com.community.repair.dto.RoleUpdateDTO;
import com.community.repair.dto.StatusUpdateDTO;
import com.community.repair.dto.UpdateProfileDTO;
import com.community.repair.dto.UserCreateDTO;
import com.community.repair.dto.UserQueryDTO;
import com.community.repair.dto.UserUpdateDTO;
import com.community.repair.vo.LoginVO;
import com.community.repair.vo.RoleChangeVO;
import com.community.repair.vo.StatusChangeVO;
import com.community.repair.vo.UserDetailVO;
import com.community.repair.vo.UserInfoVO;
import com.community.repair.vo.UserListVO;
import com.community.repair.vo.UserSimpleVO;

/**
 * 登录注册与用户管理模块 业务接口。
 *
 * <p>对应需求 FR-8-01 ~ FR-8-12，接口 U-01 ~ U-14。</p>
 *
 * <p>方法命名与接口编号的对应关系：</p>
 * <pre>
 *   U-01 register          居民账号注册（免鉴权）
 *   U-02 login             账号登录（免鉴权）
 *   U-03 logout            退出登录
 *   U-04 getCurrentUser    获取当前登录用户信息
 *   U-05 forgotPassword    忘记密码 / 自助重置密码（免鉴权）
 *   U-06 changePassword    修改本人密码
 *   U-07 updateProfile     修改本人资料
 *   U-08 pageUsers         用户分页列表（管理员）
 *   U-09 getUserDetail     用户详情（管理员）
 *   U-10 createUser        新增用户（管理员）
 *   U-11 updateUser        修改用户基本信息（管理员）
 *   U-12 changeRole        角色分配与变更（管理员）
 *   U-13 changeStatus      账号启用 / 禁用（管理员）
 *   U-14 resetPassword     重置用户密码（管理员）
 * </pre>
 */
public interface SysUserService {

    /** U-01 居民账号注册 */
    UserSimpleVO register(RegisterDTO dto);

    /** U-02 账号登录 */
    LoginVO login(LoginDTO dto);

    /** U-03 退出登录 */
    void logout(String token);

    /** U-04 获取当前登录用户信息 */
    UserInfoVO getCurrentUser();

    /** U-05 忘记密码 / 自助重置密码 */
    void forgotPassword(ForgotPasswordDTO dto);

    /** U-06 修改本人密码 */
    void changePassword(ChangePasswordDTO dto);

    /** U-07 修改本人资料 */
    UserInfoVO updateProfile(UpdateProfileDTO dto);

    /** U-08 用户分页列表 */
    PageResult<UserListVO> pageUsers(UserQueryDTO query);

    /** U-09 用户详情 */
    UserDetailVO getUserDetail(Long userId);

    /** U-10 新增用户 */
    UserSimpleVO createUser(UserCreateDTO dto);

    /** U-11 修改用户基本信息 */
    UserDetailVO updateUser(Long userId, UserUpdateDTO dto);

    /** U-12 角色分配与变更 */
    RoleChangeVO changeRole(Long userId, RoleUpdateDTO dto);

    /** U-13 账号启用 / 禁用 */
    StatusChangeVO changeStatus(Long userId, StatusUpdateDTO dto);

    /** U-14 重置用户密码 */
    void resetPassword(Long userId, ResetPasswordDTO dto);
}
