package com.community.repair.convert;

import com.community.repair.constant.UserConstants;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.vo.UserDetailVO;
import com.community.repair.vo.UserInfoVO;
import com.community.repair.vo.UserListVO;
import com.community.repair.vo.UserSimpleVO;

import java.time.LocalDateTime;

/**
 * 用户实体 -> VO 转换器。
 *
 * <p>集中处理三件事，避免在 Service 中散落重复代码：</p>
 * <ol>
 *   <li>角色编码翻译为中文名（roleName）；</li>
 *   <li>账号状态翻译为中文名（statusName）与派生字段 locked；</li>
 *   <li>保证 password 字段永远不会出现在返回对象中（BR-8-22）。</li>
 * </ol>
 */
public final class UserConverter {

    private UserConverter() {
    }

    /** 实体 -> 列表元素 VO（U-08） */
    public static UserListVO toListVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserListVO vo = new UserListVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleName(RoleEnum.nameOf(user.getRole()));
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAddress(user.getAddress());
        vo.setWorkerType(user.getWorkerType());
        vo.setSkillDesc(user.getSkillDesc());
        vo.setWorkStatus(user.getWorkStatus());
        vo.setStatus(user.getStatus());
        vo.setStatusName(statusName(user.getStatus()));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLocked(isLocked(user));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /** 实体 -> 详情 VO（U-09），在列表元素基础上追加登录安全字段 */
    public static UserDetailVO toDetailVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserDetailVO vo = new UserDetailVO();
        // 先复制父类字段
        UserListVO base = toListVO(user);
        vo.setUserId(base.getUserId());
        vo.setUsername(base.getUsername());
        vo.setRealName(base.getRealName());
        vo.setRole(base.getRole());
        vo.setRoleName(base.getRoleName());
        vo.setGender(base.getGender());
        vo.setPhone(base.getPhone());
        vo.setEmail(base.getEmail());
        vo.setAddress(base.getAddress());
        vo.setWorkerType(base.getWorkerType());
        vo.setSkillDesc(base.getSkillDesc());
        vo.setWorkStatus(base.getWorkStatus());
        vo.setStatus(base.getStatus());
        vo.setStatusName(base.getStatusName());
        vo.setLastLoginTime(base.getLastLoginTime());
        vo.setLocked(base.getLocked());
        vo.setCreateTime(base.getCreateTime());
        // 追加详情专属字段
        vo.setLoginFailCount(user.getLoginFailCount());
        vo.setLockUntil(user.getLockUntil());
        vo.setRemark(user.getRemark());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    /** 实体 -> 当前登录用户 VO（U-04、U-02 的 userInfo） */
    public static UserInfoVO toInfoVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleName(RoleEnum.nameOf(user.getRole()));
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAddress(user.getAddress());
        vo.setAvatar(user.getAvatar());
        vo.setWorkerType(user.getWorkerType());
        vo.setSkillDesc(user.getSkillDesc());
        vo.setWorkStatus(user.getWorkStatus());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /** 实体 -> 简要 VO（U-01 注册、U-10 新增用户） */
    public static UserSimpleVO toSimpleVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserSimpleVO vo = new UserSimpleVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleName(RoleEnum.nameOf(user.getRole()));
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /** 账号状态 -> 中文名 */
    public static String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == UserConstants.STATUS_ENABLED ? "启用" : "禁用";
    }

    /**
     * 是否处于登录失败锁定期：lock_until 晚于当前时间（BR-8-08）。
     */
    public static boolean isLocked(SysUser user) {
        return user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now());
    }
}
