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
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.auth.AuthService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.vo.auth.LoginVO;
import com.campus.wall.vo.auth.UserInfoVO;
import com.campus.wall.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    
    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:prod}")
    private String activeProfile;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final IdentityVerificationMapper verificationMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final AuthRuleService authRuleService;
    private final LoginLogService loginLogService;
    private final HttpServletRequest request;

    private static final String EMAIL_CODE_PREFIX = "campus:verify:email:";
    private static final long EMAIL_CODE_TTL = 5 * 60; // 5分钟

    @Override
    @Transactional
    public Long register(RegisterDTO dto) {
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

        // 部门停用禁止登录
        if (user.getDeptId() != null) {
            var dept = deptMapper.selectById(user.getDeptId());
            if (dept != null && dept.getStatus() != null && dept.getStatus() == 1) {
                recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.DEPT_DISABLED.getMessage());
                throw new BusinessException(ResultCode.DEPT_DISABLED);
            }
        }

        // 执行登录
        StpUtil.login(user.getId());
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
    public void sendEmailCode(String eduEmail) {
        // 验证EDU邮箱格式
        if (!eduEmail.endsWith(".edu.cn")) {
            throw new BusinessException("请使用EDU邮箱");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        // 存储到Redis，5分钟有效
        String key = EMAIL_CODE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, eduEmail + ":" + code, 
            EMAIL_CODE_TTL, java.util.concurrent.TimeUnit.SECONDS);

        // 实际发送邮件
        // emailService.sendVerificationCode(eduEmail, code);
        
        // 仅在开发环境下打印验证码
        if ("dev".equals(activeProfile)) {
            System.out.println("[DEV] 邮箱验证码: " + code + " -> " + eduEmail);
        }
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

    private void recordLoginLog(Long userId, String username, Integer status, String msg) {
        try {
            String ipaddr = request != null ? IpUtil.getClientIp(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            loginLogService.recordLogin(userId, username, status, msg, ipaddr, userAgent);
        } catch (Exception e) {
            // 记录登录日志失败不影响登录流程
        }
    }

    private void bindTokenSession(User user) {
        var tokenSession = StpUtil.getTokenSession();
        tokenSession.set(SecurityConstants.TOKEN_SESSION_USERNAME, user.getUsername());
        tokenSession.set(SecurityConstants.TOKEN_SESSION_NICKNAME, user.getNickname());
        String ipaddr = request != null ? IpUtil.getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
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
            String studentIdHash = SecureUtil.sha256(dto.getStudentId());
            Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .eq(User::getStudentIdHash, studentIdHash)
                    .ne(User::getId, userId)
            );
            if (existCount > 0) {
                throw new BusinessException(ResultCode.STUDENT_ID_EXISTS);
            }
        }

        // 创建审核记录
        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setImageUrl(dto.getImageUrl());
        verification.setStatus(0); // 待审核
        verificationMapper.insert(verification);

        return verification.getId();
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
}
