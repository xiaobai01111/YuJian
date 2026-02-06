package com.campus.wall.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.campus.wall.dto.auth.*;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.constant.CacheConstants;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.auth.AuthService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.service.system.SysConfigService;
import com.campus.wall.vo.auth.LoginVO;
import com.campus.wall.vo.auth.UserInfoVO;
import com.campus.wall.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final Environment environment;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final IdentityVerificationMapper verificationMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final AuthRuleService authRuleService;
    private final LoginLogService loginLogService;
    private final RateLimitService rateLimitService;
    private final SysConfigService sysConfigService;
    private final HttpServletRequest request;

    private static final String EMAIL_CODE_PREFIX = "campus:verify:email:";
    private static final String REGISTER_EMAIL_CODE_PREFIX = "campus:register:email:";
    private static final long EMAIL_CODE_TTL = 5 * 60; // 5分钟

    @Override
    @Transactional
    public Long register(RegisterDTO dto) {
        String clientIp = resolveClientIp();
        rateLimitService.checkRateLimit(
            "rate:register:ip:" + clientIp,
            RateLimitConstants.REGISTER_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );
        // 验证密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        // 检查用户名是否存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 邮箱白名单校验
        validateEmailDomain(dto.getEmail());

        // 检查邮箱是否存在
        Long emailCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail())
                .or()
                .eq(User::getEduEmail, dto.getEmail())
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被使用");
        }

        if (sysConfigService.isEmailVerificationEnabled()) {
            if (dto.getEmailCode() == null || dto.getEmailCode().isBlank()) {
                throw new BusinessException("请填写邮箱验证码");
            }
            String emailKey = REGISTER_EMAIL_CODE_PREFIX + dto.getEmail().toLowerCase();
            String cachedCode = redisTemplate.opsForValue().get(emailKey);
            if (cachedCode == null) {
                throw new BusinessException("验证码已过期，请重新获取");
            }
            if (!cachedCode.equals(dto.getEmailCode())) {
                throw new BusinessException("验证码错误");
            }
            redisTemplate.delete(emailKey);
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setVerifyStatus(0);
        user.setStatus(0);
        user.setCreditScore(100);

        userMapper.insert(user);

        // 注册后应用规则（默认分配普通用户角色等）
        authRuleService.applyRules(user, "REGISTER", null);
        return user.getId();
    }

    @Override
    @Transactional
    public LoginVO login(LoginDTO dto) {
        String clientIp = resolveClientIp();
        rateLimitService.checkRateLimit(
            "rate:login:ip:" + clientIp,
            RateLimitConstants.LOGIN_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );
        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            recordLoginLog(null, dto.getUsername(), 1, ResultCode.LOGIN_FAILED.getMessage());
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.LOGIN_FAILED.getMessage());
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 检查封禁状态
        if (user.getStatus() == 1) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.USER_BANNED.getMessage());
            throw new BusinessException(ResultCode.USER_BANNED);
        }

        if (user.getUserType() != null && user.getUserType() == 1) {
            ensureAdminBindings(user);
        }

        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        if (roleKeys == null || roleKeys.isEmpty()) {
            String msg = "账号未绑定角色，请联系管理员";
            recordLoginLog(user.getId(), user.getUsername(), 1, msg);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, msg);
        }

        // 通过角色获取部门（用户通过角色绑定部门）
        List<Long> userDeptIds = roleDeptMapper.selectDeptIdsByUserId(user.getId());
        if (userDeptIds == null || userDeptIds.isEmpty()) {
            String msg = "账号角色未绑定部门，请联系管理员";
            recordLoginLog(user.getId(), user.getUsername(), 1, msg);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, msg);
        }

        // 检查部门状态（取第一个部门进行校验）
        var dept = deptMapper.selectById(userDeptIds.getFirst());
        if (dept == null) {
            String msg = "账号部门不存在，请联系管理员";
            recordLoginLog(user.getId(), user.getUsername(), 1, msg);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, msg);
        }
        if (dept.getStatus() != null && dept.getStatus() == 1) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.DEPT_DISABLED.getMessage());
            throw new BusinessException(ResultCode.DEPT_DISABLED);
        }
        if (dept.getDataScope() == null) {
            String msg = "账号部门未配置数据权限，请联系管理员";
            recordLoginLog(user.getId(), user.getUsername(), 1, msg);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, msg);
        }
        if (dept.getDataScope() < SecurityConstants.DATA_SCOPE_ALL
            || dept.getDataScope() > SecurityConstants.DATA_SCOPE_SELF) {
            String msg = "账号部门数据权限无效，请联系管理员";
            recordLoginLog(user.getId(), user.getUsername(), 1, msg);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, msg);
        }

        // 执行登录
        StpUtil.login(user.getId(),true);
        bindTokenSession(user);
        recordOnlineToken();

        // 更新登录时间
        user.setLoginDate(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserInfo(buildUserInfoVO(user));

        recordLoginLog(user.getId(), user.getUsername(), 0, "登录成功");
        return vo;
    }

    @Override
    public void logout() {
        clearOnlineToken();
        StpUtil.logout();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return buildUserInfoVO(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(user);

        // 修改密码后登出
        StpUtil.logout();
    }

    @Override
    public void sendRegisterEmailCode(String email) {
        if (!sysConfigService.isEmailVerificationEnabled()) {
            throw new BusinessException("邮箱验证未开启");
        }
        validateEmailDomain(email);

        Long emailCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .or()
                .eq(User::getEduEmail, email)
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被使用");
        }

        String clientIp = resolveClientIp();
        String emailLower = email.toLowerCase();
        
        // 1. IP维度限流（每分钟3次）
        rateLimitService.checkRateLimit(
            "rate:register-email:ip:" + clientIp,
            RateLimitConstants.EMAIL_CODE_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );
        
        // 2. 邮箱维度冷却检查（同一邮箱60秒内只能发一次）
        String cooldownKey = "rate:register-email:cooldown:" + emailLower;
        Boolean cooldownSet = redisTemplate.opsForValue().setIfAbsent(
            cooldownKey,
            "1",
            java.time.Duration.ofSeconds(RateLimitConstants.EMAIL_CODE_COOLDOWN_SECONDS)
        );
        if (Boolean.FALSE.equals(cooldownSet)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "请稍后再试，同一邮箱" + RateLimitConstants.EMAIL_CODE_COOLDOWN_SECONDS + "秒内只能发送一次验证码");
        }
        
        // 3. 邮箱维度限流（每小时5次）
        rateLimitService.checkRateLimit(
            "rate:register-email:email:" + emailLower,
            RateLimitConstants.EMAIL_CODE_LIMIT_PER_HOUR,
            3600, // 1小时
            ResultCode.TOO_MANY_REQUESTS,
            "该邮箱验证码发送次数过多，请1小时后再试"
        );
        
        String code = RandomUtil.randomNumbers(6);
        String key = REGISTER_EMAIL_CODE_PREFIX + emailLower;
        redisTemplate.opsForValue().set(
            key,
            code,
            EMAIL_CODE_TTL,
            java.util.concurrent.TimeUnit.SECONDS
        );

        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("code", code);
        variables.put("expireMinutes", String.valueOf(EMAIL_CODE_TTL / 60));
        variables.put("email", email);
        sysConfigService.sendEmailWithTemplate(email, "verification", variables);
    }

    @Override
    public void sendEmailCode(String eduEmail) {
        // 验证EDU邮箱格式
        validateEmailDomain(eduEmail);

        Long userId = StpUtil.getLoginIdAsLong();
        rateLimitService.checkRateLimit(
            "rate:email:user:" + userId,
            RateLimitConstants.EMAIL_CODE_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );

        // 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        // 存储到Redis，5分钟有效
        String key = EMAIL_CODE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, eduEmail + ":" + code, 
            EMAIL_CODE_TTL, java.util.concurrent.TimeUnit.SECONDS);

        // 实际发送邮件
        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("code", code);
        variables.put("expireMinutes", String.valueOf(EMAIL_CODE_TTL / 60));
        variables.put("email", eduEmail);
        sysConfigService.sendEmailWithTemplate(eduEmail, "verification", variables);
        
        // 仅在开发环境下打印验证码
        if (isDevProfile()) {
            System.out.println("[DEV] 邮箱验证码: " + code + " -> " + eduEmail);
        }
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    @Override
    @Transactional
    public void confirmEmailCode(String code) {
        Long userId = StpUtil.getLoginIdAsLong();
        String key = EMAIL_CODE_PREFIX + userId;

        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        String[] parts = cached.split(":");
        if (parts.length != 2) {
            throw new BusinessException("验证码无效");
        }

        String eduEmail = parts[0];
        String savedCode = parts[1];

        if (!savedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }

        // 更新用户验证状态
        User user = userMapper.selectById(userId);
        user.setEduEmail(eduEmail);
        user.setVerifyStatus(2); // 已验证
        user.setVerifyMethod("EDU_EMAIL");
        userMapper.updateById(user);

        // 认证通过后应用规则
        authRuleService.applyRules(user, "VERIFY", "EDU_EMAIL");

        // 删除验证码
        redisTemplate.delete(key);
    }

    private void validateEmailDomain(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("邮箱不能为空");
        }
        List<String> allowedDomains = sysConfigService.getEmailAllowedDomains();
        String emailLower = email.toLowerCase();
        boolean isValidDomain = allowedDomains.stream()
            .filter(domain -> domain != null && !domain.isBlank())
            .anyMatch(domain -> {
                String normalized = domain.trim().toLowerCase();
                return emailLower.endsWith("@" + normalized) || emailLower.endsWith("." + normalized);
            });
        if (!isValidDomain) {
            throw new BusinessException("请使用允许的邮箱域名（支持的域名：" + String.join(", ", allowedDomains) + "）");
        }
    }

    private void recordLoginLog(Long userId, String username, Integer status, String msg) {
        try {
            String ipaddr = request != null ? IpUtil.getClientIp(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            loginLogService.recordLogin(userId, username, status, msg, ipaddr, userAgent);
        } catch (Exception e) {
            // 记录登录日志失败不影响登录流程
        }
    }

    private String resolveClientIp() {
        String ip = request != null ? IpUtil.getClientIp(request) : null;
        return (ip == null || ip.isBlank()) ? "unknown" : ip;
    }

    private void bindTokenSession(User user) {
        var tokenSession = StpUtil.getTokenSession();
        tokenSession.set(SecurityConstants.TOKEN_SESSION_USERNAME, user.getUsername());
        tokenSession.set(SecurityConstants.TOKEN_SESSION_NICKNAME, user.getNickname());
        String ipaddr = resolveClientIp();
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "unknown";
        }
        tokenSession.set(SecurityConstants.TOKEN_SESSION_IP, ipaddr);
        tokenSession.set(SecurityConstants.TOKEN_SESSION_USER_AGENT, userAgent);
        tokenSession.set(SecurityConstants.TOKEN_SESSION_LOGIN_TIME, java.time.LocalDateTime.now().toString());
    }

    private void recordOnlineToken() {
        try {
            String token = StpUtil.getTokenValue();
            if (token != null && !token.isBlank()) {
                redisTemplate.opsForSet().add(CacheConstants.ONLINE_TOKENS, token);
            }
        } catch (Exception e) {
            // 在线列表写入失败不影响登录
        }
    }

    private void clearOnlineToken() {
        try {
            String token = StpUtil.getTokenValue();
            if (token != null && !token.isBlank()) {
                redisTemplate.opsForSet().remove(CacheConstants.ONLINE_TOKENS, token);
            }
        } catch (Exception e) {
            // 在线列表清理失败不影响登出
        }
    }

    @Override
    @Transactional
    public Long submitIdCard(SubmitIdCardDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查是否有待审核的记录
        Long pendingCount = verificationMapper.selectCount(
            new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getUserId, userId)
                .eq(IdentityVerification::getStatus, 0)
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的申请，请耐心等待");
        }

        // 如果提供了学号，检查唯一性
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            assertStudentIdAvailable(dto.getStudentId(), userId);
        }

        // 创建审核记录
        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setImageUrl(dto.getImageUrl());
        verification.setVerifyMethod("ID_CARD");
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            verification.setStudentId(dto.getStudentId());
            verification.setStudentIdHash(SecureUtil.sha256(dto.getStudentId()));
        }
        verification.setStatus(0); // 待审核
        verificationMapper.insert(verification);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setVerifyStatus(1); // 审核中
            user.setVerifyMethod("ID_CARD");
            userMapper.updateById(user);
        }

        return verification.getId();
    }

    @Override
    @Transactional
    public Long submitStudentId(SubmitStudentIdDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        Long pendingCount = verificationMapper.selectCount(
            new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getUserId, userId)
                .eq(IdentityVerification::getStatus, 0)
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的申请，请耐心等待");
        }

        assertStudentIdAvailable(dto.getStudentId(), userId);

        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setVerifyMethod("ID_LIST");
        verification.setStudentId(dto.getStudentId());
        verification.setStudentIdHash(SecureUtil.sha256(dto.getStudentId()));
        verification.setStatus(0);
        verificationMapper.insert(verification);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setVerifyStatus(1);
            user.setVerifyMethod("ID_LIST");
            userMapper.updateById(user);
        }

        return verification.getId();
    }

    @Override
    @Transactional
    public void cancelVerification() {
        Long userId = StpUtil.getLoginIdAsLong();
        IdentityVerification verification = verificationMapper.selectOne(
            new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getUserId, userId)
                .eq(IdentityVerification::getStatus, 0)
                .orderByDesc(IdentityVerification::getCreatedAt)
                .last("LIMIT 1")
        );
        if (verification == null) {
            throw new BusinessException("没有待审核的认证申请");
        }

        verification.setStatus(3);
        verification.setReviewedAt(java.time.LocalDateTime.now());
        verificationMapper.updateById(verification);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setVerifyStatus(0);
            user.setVerifyMethod(null);
            userMapper.updateById(user);
        }
    }

    private void assertStudentIdAvailable(String studentId, Long userId) {
        String studentIdHash = SecureUtil.sha256(studentId);
        Long existCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getStudentIdHash, studentIdHash)
                .ne(User::getId, userId)
        );
        if (existCount > 0) {
            throw new BusinessException(ResultCode.STUDENT_ID_EXISTS);
        }

        Long pendingCount = verificationMapper.selectCount(
            new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getStudentIdHash, studentIdHash)
                .eq(IdentityVerification::getStatus, 0)
                .ne(IdentityVerification::getUserId, userId)
        );
        if (pendingCount > 0) {
            throw new BusinessException("该学号已存在待审核申请");
        }
    }

    private UserInfoVO buildUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setVerifyStatus(user.getVerifyStatus());
        vo.setVerifyMethod(user.getVerifyMethod());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreatedAt(user.getCreatedAt());

        // 获取角色和权限
        List<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions;
        
        // 超级管理员拥有所有权限
        if (roles.contains(SecurityUtil.getSuperAdminRoleKey())) {
            permissions = new java.util.ArrayList<>();
            permissions.add(SecurityConstants.ALL_PERMISSION);
        } else {
            permissions = menuMapper.selectPermsByUserId(user.getId());
        }
        
        vo.setRoles(roles);
        vo.setPermissions(permissions);

        return vo;
    }

    private void ensureAdminBindings(User user) {
        var adminRole = roleMapper.selectByRoleKey(SecurityUtil.getSuperAdminRoleKey());
        if (adminRole == null) {
            return;
        }

        if (user.getDeptId() == null) {
            user.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            userMapper.updateById(user);
        }

        Long userRoleCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<com.campus.wall.entity.system.SysUserRole>()
                        .eq(com.campus.wall.entity.system.SysUserRole::getUserId, user.getId())
                        .eq(com.campus.wall.entity.system.SysUserRole::getRoleId, adminRole.getId())
        );
        if (userRoleCount == null || userRoleCount == 0) {
            com.campus.wall.entity.system.SysUserRole userRole = new com.campus.wall.entity.system.SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
        }

        List<Long> roleDeptIds = roleDeptMapper.selectDeptIdsByRoleId(adminRole.getId());
        if (roleDeptIds == null || !roleDeptIds.contains(SecurityConstants.SYSTEM_DEPT_ID)) {
            com.campus.wall.entity.system.SysRoleDept roleDept = new com.campus.wall.entity.system.SysRoleDept();
            roleDept.setRoleId(adminRole.getId());
            roleDept.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            roleDeptMapper.insert(roleDept);
        }

        List<Long> roleMenuIds = menuMapper.selectMenuIdsByRoleId(adminRole.getId());
        java.util.Set<Long> existing = roleMenuIds == null ? new java.util.HashSet<>() : new java.util.HashSet<>(roleMenuIds);
        List<com.campus.wall.entity.system.SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<com.campus.wall.entity.system.SysMenu>().select(com.campus.wall.entity.system.SysMenu::getId)
        );
        for (com.campus.wall.entity.system.SysMenu menu : menus) {
            if (menu.getId() == null || existing.contains(menu.getId())) {
                continue;
            }
            com.campus.wall.entity.system.SysRoleMenu roleMenu = new com.campus.wall.entity.system.SysRoleMenu();
            roleMenu.setRoleId(adminRole.getId());
            roleMenu.setMenuId(menu.getId());
            roleMenuMapper.insert(roleMenu);
        }
    }

    @Override
    public com.campus.wall.vo.auth.AdminContactVO getAdminContact() {
        com.campus.wall.vo.auth.AdminContactVO vo = new com.campus.wall.vo.auth.AdminContactVO();
        // 查找超级管理员角色的用户
        String adminRoleKey = SecurityUtil.getSuperAdminRoleKey();
        var adminRole = roleMapper.selectOne(
            new LambdaQueryWrapper<com.campus.wall.entity.system.SysRole>()
                .eq(com.campus.wall.entity.system.SysRole::getRoleKey, adminRoleKey)
        );
        if (adminRole != null) {
            // 通过角色ID查找用户
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(adminRole.getId());
            if (userIds != null && !userIds.isEmpty()) {
                User admin = userMapper.selectById(userIds.getFirst());
                if (admin != null) {
                    vo.setEmail(admin.getEmail());
                    vo.setPhone(admin.getPhone());
                }
            }
        }
        return vo;
    }
}
