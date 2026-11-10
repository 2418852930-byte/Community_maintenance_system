package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.UserConstants;
import com.community.repair.convert.UserConverter;
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
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.JwtUtil;
import com.community.repair.security.TokenBlacklist;
import com.community.repair.security.UserContext;
import com.community.repair.service.SysUserService;
import com.community.repair.util.PasswordUtil;
import com.community.repair.vo.LoginVO;
import com.community.repair.vo.RoleChangeVO;
import com.community.repair.vo.StatusChangeVO;
import com.community.repair.vo.UserDetailVO;
import com.community.repair.vo.UserInfoVO;
import com.community.repair.vo.UserListVO;
import com.community.repair.vo.UserSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 登录注册与用户管理模块 业务实现。
 *
 * <p>本类集中承载需求文档 5.8.6 节 BR-8-01 ~ BR-8-22 的全部业务规则，
 * 每个方法上方的注释都标注了对应的规则编号，便于论文与答辩溯源。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    /** 查询条件中的时间格式，与接口文档 2.5 节一致 */
    private static final DateTimeFormatter QUERY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 分页查询每页条数上限（接口文档 2.5 节：最大 100） */
    private static final int MAX_PAGE_SIZE = 100;

    // ==================================================================================
    // 一、认证类接口（U-01 ~ U-07）
    // ==================================================================================

    /**
     * U-01 居民账号注册。
     *
     * <p>对应 FR-8-01、BR-8-01 ~ BR-8-06、BR-8-22。</p>
     *
     * <p>关键点：角色固定写死为 resident，<b>不接受前端传入</b>；账号状态默认启用；
     * 密码以 BCrypt 加密后写入；注册成功后不自动登录、不签发 JWT。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSimpleVO register(RegisterDTO dto) {
        // BR-8-03 密码强度：6~20 位且同时包含字母与数字
        if (!PasswordUtil.isValidStrength(dto.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PASSWORD_RULE);
        }
        // BR-8-04 两次输入的密码必须完全一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        // BR-8-02 用户名全局唯一
        if (existsByUsername(dto.getUsername())) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_USERNAME_EXISTS);
        }
        // BR-8-01 手机号全局唯一，是账号唯一标识
        if (existsByPhone(dto.getPhone())) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_PHONE_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // BR-8-22 密码 BCrypt 加密存储
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        // BR-8-06 系统只对居民开放自助注册入口，角色固定写死
        user.setRole(RoleEnum.RESIDENT.getCode());
        user.setGender(0);
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        // BR-8-05 楼栋房号作为报修工单「报修位置」的默认值
        user.setAddress(dto.getAddress());
        user.setStatus(UserConstants.STATUS_ENABLED);
        user.setLoginFailCount(0);
        user.setDeleted(0);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时的兜底：数据库 uk_username / uk_phone 唯一约束生效
            log.warn("注册触发唯一索引冲突：username={}, phone={}", dto.getUsername(), dto.getPhone());
            throw new BusinessException(ResultCode.CONFLICT, "该用户名或手机号已被注册");
        }
        log.info("居民注册成功：userId={}, username={}", user.getUserId(), user.getUsername());
        return UserConverter.toSimpleVO(user);
    }

    /**
     * U-02 账号登录。
     *
     * <p>对应 FR-8-02、BR-8-07 ~ BR-8-10。</p>
     *
     * <p>校验顺序（严格对齐接口文档 4.1.2）：① 账号是否存在 → ② 账号是否被锁定 →
     * ③ 账号状态是否启用 → ④ 密码 BCrypt 比对。</p>
     *
     * <p><b>注意</b>：本方法<b>不加 @Transactional</b>。因为登录失败时需要先累加
     * login_fail_count 再抛出异常，若包在事务里，异常会触发回滚导致失败次数丢失。</p>
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = findByAccount(dto.getAccount());
        // ① 账号不存在：统一提示，不暴露账号是否存在，避免账号枚举
        if (user == null) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_LOGIN_FAIL);
        }

        LocalDateTime now = LocalDateTime.now();

        // ② 账号是否处于锁定期（BR-8-08）
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(now)) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_ACCOUNT_LOCKED);
        }
        // 锁定时间已过 —— 自动解锁，失败计数清零
        if (user.getLockUntil() != null) {
            user.setLockUntil(null);
            user.setLoginFailCount(0);
        }

        // ③ 账号状态是否启用（BR-8-09）
        if (user.getStatus() == null || user.getStatus() != UserConstants.STATUS_ENABLED) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_ACCOUNT_DISABLED);
        }

        // ④ 密码比对（BCrypt）
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            handleLoginFailure(user, now);
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_LOGIN_FAIL);
        }

        // 登录成功：更新最后登录时间、失败计数清零、锁定状态清空（BR-8-08）
        // 说明：lock_until 需要显式置为 NULL，而 updateById 的默认策略会跳过 null 字段，
        // 因此这里改用 LambdaUpdateWrapper 精确控制 SET 子句。
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, user.getUserId())
                .set(SysUser::getLastLoginTime, now)
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));

        // 回写内存对象，保证返回给前端的 userInfo 是登录后的最新状态
        user.setLastLoginTime(now);
        user.setLoginFailCount(0);
        user.setLockUntil(null);

        // BR-8-07 签发 JWT，载荷含 userId 与 role，有效期 7200 秒
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn((int) jwtUtil.getExpireSeconds());
        vo.setUserInfo(UserConverter.toInfoVO(user));
        log.info("用户登录成功：userId={}, username={}, role={}", user.getUserId(), user.getUsername(), user.getRole());
        return vo;
    }

    /**
     * U-03 退出登录。
     *
     * <p>对应 FR-8-03。JWT 无状态，服务端把令牌加入黑名单直到其自然过期；前端同时清除本地令牌。</p>
     */
    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            tokenBlacklist.add(token, jwtUtil.getExpireAtMillis(token));
            log.info("用户退出登录，令牌已加入黑名单：userId={}", UserContext.get() == null ? null : UserContext.get().getUserId());
        }
    }

    /**
     * U-04 获取当前登录用户信息。
     *
     * <p>对应 FR-8-06。从 JWT 解析用户 ID，返回本人完整档案；不返回 password 字段（BR-8-22）。</p>
     */
    @Override
    public UserInfoVO getCurrentUser() {
        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserConverter.toInfoVO(user);
    }

    /**
     * U-05 忘记密码 / 自助重置密码。
     *
     * <p>对应 FR-8-04、BR-8-11、BR-8-12。</p>
     *
     * <p>须同时校验用户名与注册预留手机号；二者不匹配时统一提示，
     * 不暴露账号是否存在。重置成功后清零登录失败计数与锁定状态。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordDTO dto) {
        // 新密码强度校验（BR-8-03）
        if (!PasswordUtil.isValidStrength(dto.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PASSWORD_RULE);
        }
        // 两次密码一致性（BR-8-04）
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        SysUser user = findByUsername(dto.getUsername());
        // BR-8-11 用户名与预留手机号必须匹配；不匹配时统一提示
        if (user == null || !dto.getPhone().equals(user.getPhone())) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_FORGOT_NOT_MATCH);
        }
        // BR-8-12 新密码不得与原密码相同
        if (PasswordUtil.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.CONFLICT, "新密码不能与原密码相同");
        }

        // 重置密码，并清除登录失败计数与锁定状态（lock_until 需显式置 NULL）
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, user.getUserId())
                .set(SysUser::getPassword, PasswordUtil.encode(dto.getNewPassword()))
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));
        log.info("自助重置密码成功：userId={}", user.getUserId());
    }

    /**
     * U-06 修改本人密码。
     *
     * <p>对应 FR-8-05、BR-8-13：用户只能修改本人密码，且必须校验原密码是否正确。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        if (!PasswordUtil.isValidStrength(dto.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PASSWORD_RULE);
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        Long userId = UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // BR-8-13 必须校验原密码
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.CONFLICT, "原密码错误");
        }
        if (PasswordUtil.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.CONFLICT, "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        sysUserMapper.updateById(update);
        log.info("用户修改本人密码成功：userId={}", userId);
    }

    /**
     * U-07 修改本人资料。
     *
     * <p>对应 FR-8-06、BR-8-13。</p>
     *
     * <p>不可修改：username、role、status；<b>手机号 phone 作为账号唯一标识不允许自行修改</b>；
     * workerType / skillDesc / workStatus 仅维修人员可提交；address 仅居民可提交。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateProfile(UpdateProfileDTO dto) {
        // 只读哨兵：提交了这些字段一律拒绝
        if (dto.getPhone() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PHONE_READONLY);
        }
        if (dto.getUsername() != null || dto.getRole() != null || dto.getStatus() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "登录账号、角色、账号状态不支持自行修改");
        }

        Long userId = UserContext.getUserId();
        String currentRole = UserContext.getRole();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 维修人员专属字段的角色校验
        boolean hasRepairField = dto.getWorkerType() != null || dto.getSkillDesc() != null || dto.getWorkStatus() != null;
        if (hasRepairField && !RoleEnum.REPAIR.getCode().equals(currentRole)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前角色无权修改维修人员专属信息");
        }

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setRealName(dto.getRealName());
        update.setGender(dto.getGender());
        update.setEmail(dto.getEmail());
        update.setAvatar(dto.getAvatar());
        // address 仅居民使用（BR-8-05）
        if (RoleEnum.RESIDENT.getCode().equals(currentRole)) {
            update.setAddress(dto.getAddress());
        }
        if (RoleEnum.REPAIR.getCode().equals(currentRole)) {
            update.setWorkerType(dto.getWorkerType());
            update.setSkillDesc(dto.getSkillDesc());
            update.setWorkStatus(dto.getWorkStatus());
        }
        // 说明：MyBatis-Plus 默认字段策略为 NOT_NULL，null 字段不会进入 UPDATE 语句，
        // 因此这里天然实现了「只更新传了的字段」的局部更新语义。
        sysUserMapper.updateById(update);

        SysUser latest = sysUserMapper.selectById(userId);
        return UserConverter.toInfoVO(latest);
    }

    // ==================================================================================
    // 二、用户管理类接口（U-08 ~ U-14，仅系统管理员）
    // ==================================================================================

    /**
     * U-08 用户分页列表。
     *
     * <p>对应 FR-8-07。支持关键字（用户名 / 真实姓名 / 手机号）模糊查询，
     * 并按角色、账号状态、工种、工作状态、创建时间区间组合筛选。</p>
     */
    @Override
    public PageResult<UserListVO> pageUsers(UserQueryDTO query) {
        // 角色取值合法性校验（接口文档 400 场景）
        if (StringUtils.hasText(query.getRole()) && !RoleEnum.isValid(query.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色取值非法，只能是 resident / repair / admin");
        }

        // 分页参数保护：pageNum >= 1，1 <= pageSize <= 100
        int pageNum = (query.getPageNum() == null || query.getPageNum() < 1) ? 1 : query.getPageNum();
        int pageSize = (query.getPageSize() == null || query.getPageSize() < 1) ? 10 : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 关键字：username / real_name / phone 三者任一模糊匹配，用嵌套 and(...) 保证 OR 的优先级正确
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getRole()), SysUser::getRole, query.getRole());
        wrapper.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getWorkerType()), SysUser::getWorkerType, query.getWorkerType());
        wrapper.eq(query.getWorkStatus() != null, SysUser::getWorkStatus, query.getWorkStatus());
        // 创建时间区间
        if (StringUtils.hasText(query.getStartTime())) {
            wrapper.ge(SysUser::getCreateTime, parseQueryTime(query.getStartTime()));
        }
        if (StringUtils.hasText(query.getEndTime())) {
            wrapper.le(SysUser::getCreateTime, parseQueryTime(query.getEndTime()));
        }
        applyOrder(wrapper, query.getOrderBy(), query.getOrderDir());

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, UserConverter::toListVO);
    }

    /**
     * U-09 用户详情。
     *
     * <p>对应 FR-8-08。返回基础信息、角色、账号状态、创建时间、最近登录时间与登录安全状态。</p>
     */
    @Override
    public UserDetailVO getUserDetail(Long userId) {
        SysUser user = selectExistingUser(userId);
        return UserConverter.toDetailVO(user);
    }

    /**
     * U-10 新增用户（维修人员 / 系统管理员）。
     *
     * <p>对应 FR-8-09、BR-8-15、BR-8-16。</p>
     *
     * <p><b>本入口不提供居民角色</b>：居民只能通过 U-01 自助注册，提交 role = resident 返回 400。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSimpleVO createUser(UserCreateDTO dto) {
        // BR-8-16 入口只允许创建 repair / admin
        if (!RoleEnum.isValid(dto.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色取值非法，只能是 repair / admin");
        }
        if (RoleEnum.RESIDENT.getCode().equals(dto.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "本入口不提供居民角色，居民请通过注册页面自助注册");
        }
        // BR-8-03 密码强度
        if (!PasswordUtil.isValidStrength(dto.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PASSWORD_RULE);
        }
        // BR-8-02 / BR-8-01 唯一性
        if (existsByUsername(dto.getUsername())) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_USERNAME_EXISTS);
        }
        if (existsByPhone(dto.getPhone())) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_PHONE_EXISTS);
        }

        boolean isRepair = RoleEnum.REPAIR.getCode().equals(dto.getRole());
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (isRepair) {
            // BR-8-15 维修人员：工种与技能描述选填，工作状态默认 1 空闲
            user.setWorkerType(dto.getWorkerType());
            user.setSkillDesc(dto.getSkillDesc());
            user.setWorkStatus(dto.getWorkStatus() == null ? 1 : dto.getWorkStatus());
        }
        user.setRemark(dto.getRemark());
        // BR-8-15 新账号状态默认「启用」
        user.setStatus(UserConstants.STATUS_ENABLED);
        user.setLoginFailCount(0);
        user.setDeleted(0);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            log.warn("新增用户触发唯一索引冲突：username={}, phone={}", dto.getUsername(), dto.getPhone());
            throw new BusinessException(ResultCode.CONFLICT, "该用户名或手机号已被使用");
        }
        log.info("管理员新增用户成功：userId={}, username={}, role={}, 操作人={}",
                user.getUserId(), user.getUsername(), user.getRole(), UserContext.getUserId());
        return UserConverter.toSimpleVO(user);
    }

    /**
     * U-11 修改用户基本信息。
     *
     * <p>对应 FR-8-10、BR-8-14。<b>注册手机号 phone 与 create_time 不可修改</b>，
     * 请求体中出现即返回 400；username 亦不支持修改。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetailVO updateUser(Long userId, UserUpdateDTO dto) {
        // BR-8-14 只读字段校验
        if (dto.getPhone() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "注册手机号不可修改");
        }
        if (dto.getUsername() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "登录账号不可修改");
        }
        if (dto.getCreateTime() != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "创建时间不可修改");
        }

        SysUser user = selectExistingUser(userId);
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setRealName(dto.getRealName());
        update.setGender(dto.getGender());
        update.setEmail(dto.getEmail());
        update.setRemark(dto.getRemark());
        // 工种类字段只在目标用户是维修人员时才允许写入，避免给居民/管理员写入无意义数据
        if (RoleEnum.REPAIR.getCode().equals(user.getRole())) {
            update.setWorkerType(dto.getWorkerType());
            update.setSkillDesc(dto.getSkillDesc());
            update.setWorkStatus(dto.getWorkStatus());
        }
        // 楼栋房号只在居民账号上维护
        if (RoleEnum.RESIDENT.getCode().equals(user.getRole())) {
            update.setAddress(dto.getAddress());
        }
        sysUserMapper.updateById(update);
        log.info("管理员修改用户信息成功：userId={}, 操作人={}", userId, UserContext.getUserId());
        return UserConverter.toDetailVO(sysUserMapper.selectById(userId));
    }

    /**
     * U-12 角色分配与变更。
     *
     * <p>对应 FR-8-11、BR-8-17、BR-8-18。</p>
     *
     * <p>约束：① 管理员不可变更本人账号的角色；② 变更后系统须至少保留一个启用的管理员账号；
     * ③ 变更在下一次请求即生效（服务端以数据库最新角色校验）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleChangeVO changeRole(Long userId, RoleUpdateDTO dto) {
        if (!RoleEnum.isValid(dto.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色取值非法，只能是 resident / repair / admin");
        }
        // BR-8-18 不可变更本人账号的角色
        if (userId.equals(UserContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, UserConstants.MSG_CANNOT_CHANGE_SELF_ROLE);
        }

        SysUser user = selectExistingUser(userId);
        String oldRole = user.getRole();
        String newRole = dto.getRole();

        if (oldRole.equalsIgnoreCase(newRole)) {
            // 接口文档 2.1 节：PUT 按幂等设计，重复提交同一变更返回 409
            throw new BusinessException(ResultCode.CONFLICT, "该用户当前已是目标角色，请勿重复提交");
        }
        // BR-8-18 降级最后一个启用的管理员 -> 拒绝
        if (RoleEnum.ADMIN.getCode().equals(oldRole)
                && user.getStatus() != null && user.getStatus() == UserConstants.STATUS_ENABLED
                && !RoleEnum.ADMIN.getCode().equals(newRole)
                && countEnabledAdmin() <= 1) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_KEEP_LAST_ADMIN);
        }

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setRole(newRole);
        sysUserMapper.updateById(update);

        SysUser latest = sysUserMapper.selectById(userId);
        RoleChangeVO vo = new RoleChangeVO();
        vo.setUserId(latest.getUserId());
        vo.setUsername(latest.getUsername());
        vo.setRealName(latest.getRealName());
        vo.setOldRole(oldRole);
        vo.setOldRoleName(RoleEnum.nameOf(oldRole));
        vo.setRole(latest.getRole());
        vo.setRoleName(RoleEnum.nameOf(latest.getRole()));
        vo.setUpdateTime(latest.getUpdateTime());
        vo.setTip("角色变更已生效，该用户下次登录后菜单将同步刷新");
        log.info("管理员变更用户角色：userId={}, {} -> {}, 操作人={}", userId, oldRole, newRole, UserContext.getUserId());
        return vo;
    }

    /**
     * U-13 账号启用 / 禁用。
     *
     * <p>对应 FR-8-12、BR-8-09、BR-8-18、BR-8-19。</p>
     *
     * <p>约束：① 管理员不得禁用本人账号；② 不得禁用系统中最后一个启用的管理员；
     * ③ 禁用即时生效，既有令牌下次请求即被拒绝。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StatusChangeVO changeStatus(Long userId, StatusUpdateDTO dto) {
        Integer targetStatus = dto.getStatus();
        // BR-8-18 不可禁用本人账号
        if (userId.equals(UserContext.getUserId()) && targetStatus == UserConstants.STATUS_DISABLED) {
            throw new BusinessException(ResultCode.FORBIDDEN, UserConstants.MSG_CANNOT_DISABLE_SELF);
        }

        SysUser user = selectExistingUser(userId);
        if (targetStatus.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该账号当前已是目标状态，请勿重复提交");
        }
        // BR-8-18 禁用最后一个启用的管理员 -> 拒绝
        if (targetStatus == UserConstants.STATUS_DISABLED
                && RoleEnum.ADMIN.getCode().equals(user.getRole())
                && countEnabledAdmin() <= 1) {
            throw new BusinessException(ResultCode.CONFLICT, UserConstants.MSG_KEEP_LAST_ADMIN);
        }

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setStatus(targetStatus);
        sysUserMapper.updateById(update);

        SysUser latest = sysUserMapper.selectById(userId);
        StatusChangeVO vo = new StatusChangeVO();
        vo.setUserId(latest.getUserId());
        vo.setUsername(latest.getUsername());
        vo.setStatus(latest.getStatus());
        vo.setStatusName(UserConverter.statusName(latest.getStatus()));
        vo.setUpdateTime(latest.getUpdateTime());
        // BR-8-19 被禁用维修人员的在途工单不做自动回收，由管理员在派单模块执行改派
        if (targetStatus == UserConstants.STATUS_DISABLED && RoleEnum.REPAIR.getCode().equals(latest.getRole())) {
            vo.setTip("该维修人员已禁用，其在途工单请到「工单派单调度」模块执行改派");
        }
        log.info("管理员变更账号状态：userId={}, status={}, 操作人={}", userId, targetStatus, UserContext.getUserId());
        return vo;
    }

    /**
     * U-14 重置用户密码。
     *
     * <p>对应 BR-8-21：新密码须满足密码强度规则；重置成功后清零登录失败计数与锁定状态。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, ResetPasswordDTO dto) {
        if (!PasswordUtil.isValidStrength(dto.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, UserConstants.MSG_PASSWORD_RULE);
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        selectExistingUser(userId);

        // 重置密码同时解锁，避免用户拿到新密码却仍被锁定（BR-8-21 + BR-8-08）
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getPassword, PasswordUtil.encode(dto.getNewPassword()))
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));
        log.info("管理员重置用户密码：userId={}, 操作人={}", userId, UserContext.getUserId());
    }

    // ==================================================================================
    // 三、私有工具方法
    // ==================================================================================

    /**
     * 登录失败处理：失败计数自增，累计达 5 次则把锁定截止时间置为「当前时间 + 10 分钟」（BR-8-08）。
     *
     * @param user 当前用户
     * @param now  本次失败时间
     */
    private void handleLoginFailure(SysUser user, LocalDateTime now) {
        int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setLoginFailCount(failCount);
        if (failCount >= UserConstants.MAX_LOGIN_FAIL_COUNT) {
            update.setLockUntil(now.plusMinutes(UserConstants.LOCK_MINUTES));
            log.warn("账号连续登录失败达 {} 次，已锁定 {} 分钟：userId={}",
                    UserConstants.MAX_LOGIN_FAIL_COUNT, UserConstants.LOCK_MINUTES, user.getUserId());
        }
        sysUserMapper.updateById(update);
        log.info("登录失败：userId={}, 累计失败次数={}", user.getUserId(), failCount);
    }

    /**
     * 按账号查询用户：先按 username 精确匹配，再按 phone 精确匹配（BR-8-01 手机号可登录）。
     */
    private SysUser findByAccount(String account) {
        SysUser user = findByUsername(account);
        if (user == null) {
            user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, account)
                    .last("LIMIT 1"));
        }
        return user;
    }

    /** 按用户名精确查询（逻辑删除的记录自动排除） */
    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    }

    /** 用户名是否已存在（BR-8-02） */
    private boolean existsByUsername(String username) {
        return sysUserMapper.exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    /** 手机号是否已存在（BR-8-01） */
    private boolean existsByPhone(String phone) {
        return sysUserMapper.exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
    }

    /** 查询用户，不存在则抛 404 */
    private SysUser selectExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或已被删除");
        }
        return user;
    }

    /** 统计系统中「启用」状态的管理员数量（BR-8-18 最后一个管理员保护） */
    private long countEnabledAdmin() {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, RoleEnum.ADMIN.getCode())
                .eq(SysUser::getStatus, UserConstants.STATUS_ENABLED));
        return count == null ? 0L : count;
    }

    /**
     * 解析查询条件中的时间字符串，格式 yyyy-MM-dd HH:mm:ss。
     */
    private LocalDateTime parseQueryTime(String text) {
        try {
            return LocalDateTime.parse(text.trim(), QUERY_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 排序字段白名单。
     *
     * <p>采用白名单而非直接把前端字符串拼进 SQL，从根源上避免 SQL 注入。
     * 未命中白名单时按创建时间倒序。</p>
     */
    private void applyOrder(LambdaQueryWrapper<SysUser> wrapper, String orderBy, String orderDir) {
        boolean asc = "asc".equalsIgnoreCase(orderDir);
        String field = orderBy == null ? "" : orderBy.trim();
        switch (field) {
            case "username" -> wrapper.orderBy(true, asc, SysUser::getUsername);
            case "real_name" -> wrapper.orderBy(true, asc, SysUser::getRealName);
            case "last_login_time" -> wrapper.orderBy(true, asc, SysUser::getLastLoginTime);
            case "user_id" -> wrapper.orderBy(true, asc, SysUser::getUserId);
            case "create_time" -> wrapper.orderBy(true, asc, SysUser::getCreateTime);
            default -> wrapper.orderByDesc(SysUser::getCreateTime);
        }
    }
}
