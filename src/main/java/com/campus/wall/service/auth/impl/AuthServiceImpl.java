package com.campus.wall.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import cn.hutool.core.util.RandomUtil;
import com.campus.wall.dto.auth.*;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.constant.CacheConstants;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.enums.file.FileAuditStatus;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.auth.AuthService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.system.BlocklistService;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.service.system.OnlineUserService;
import com.campus.wall.service.system.SysConfigService;
import com.campus.wall.vo.auth.LoginCaptchaVO;
import com.campus.wall.vo.auth.LoginVO;
import com.campus.wall.vo.auth.UserInfoVO;
import com.campus.wall.util.DeviceIdUtil;
import com.campus.wall.util.IpUtil;
import com.campus.wall.util.PasswordPolicyUtil;
import com.campus.wall.util.SensitiveFieldUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

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
    private final IdentityVerificationMapper verificationMapper;
    private final FileRecordMapper fileRecordMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final AuthRuleService authRuleService;
    private final BlocklistService blocklistService;
    private final LoginLogService loginLogService;
    private final RateLimitService rateLimitService;
    private final SysConfigService sysConfigService;
    private final OnlineUserService onlineUserService;
    private final HttpServletRequest request;

    private static final String EMAIL_CODE_PREFIX = "campus:verify:email:";
    private static final String REGISTER_EMAIL_CODE_PREFIX = "campus:register:email:";
    private static final String LOGIN_CAPTCHA_PREFIX = "campus:login:captcha:";
    private static final String LOGIN_FAIL_USER_PREFIX = "campus:login:fail:user:";
    private static final String LOGIN_FAIL_DEVICE_PREFIX = "campus:login:fail:device:";
    private static final String LOGIN_LOCK_USER_PREFIX = "campus:login:lock:user:";
    private static final String LOGIN_LOCK_DEVICE_PREFIX = "campus:login:lock:device:";
    private static final String LOGIN_REFRESH_TOKEN_PREFIX = "campus:login:refresh:";
    private static final String LOGIN_REFRESH_USER_INDEX_PREFIX = "campus:login:refresh:user:";
    private static final long EMAIL_CODE_TTL = 5 * 60; // 5分钟
    private static final String BLOCKLIST_TYPE_USER = "USER";
    private static final String TARGET_TYPE_ID_CARD = FileTargetType.ID_CARD.getCode();
    private static final int LOGIN_USERNAME_MAX_LENGTH = 50;
    private static final int LOGIN_PASSWORD_MAX_LENGTH = 128;
    private static final int REFRESH_TOKEN_MAX_LENGTH = 128;
    private static final char[] LOGIN_CAPTCHA_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LOGIN_CAPTCHA_CODE_LENGTH = 5;
    private static final int LOGIN_CAPTCHA_IMAGE_WIDTH = 150;
    private static final int LOGIN_CAPTCHA_IMAGE_HEIGHT = 52;
    private static final String LOGIN_CAPTCHA_PROMPT = "请输入图形验证码";
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final String[] DEVICE_ID_HEADERS = {
        "X-Device-Id", "X-Device-ID", "X-Client-Id", "X-Client-ID", "Device-Id", "Device-ID"
    };

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
        PasswordPolicyUtil.validateOrThrow(dto.getPassword());

        // 检查用户名是否存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        String normalizedEmail = SensitiveFieldUtil.normalizeEmail(dto.getEmail());

        // 邮箱白名单校验
        validateEmailDomain(normalizedEmail);
        String emailHash = SensitiveFieldUtil.hashEmail(normalizedEmail);

        // 检查邮箱是否存在
        Long emailCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getEmailHash, emailHash)
                    .or()
                    .eq(User::getEduEmailHash, emailHash)
                    .or()
                    .eq(User::getEmail, normalizedEmail)
                    .or()
                    .eq(User::getEduEmail, normalizedEmail))
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被使用");
        }

        if (sysConfigService.isEmailVerificationEnabled()) {
            if (dto.getEmailCode() == null || dto.getEmailCode().isBlank()) {
                throw new BusinessException("请填写邮箱验证码");
            }
            String emailKey = REGISTER_EMAIL_CODE_PREFIX + normalizedEmail;
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
        user.setEmail(normalizedEmail);
        user.setEmailHash(emailHash);
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
        String loginUsername = normalizeLoginUsername(dto.getUsername());
        validateLoginPayload(loginUsername, dto.getPassword());
        String clientIp = resolveClientIp();
        String deviceId = resolveDeviceId();
        rateLimitService.checkRateLimit(
            "rate:login:ip:" + clientIp,
            RateLimitConstants.LOGIN_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );
        if (loginUsername != null) {
            rateLimitService.checkRateLimit(
                "rate:login:user:" + loginUsername,
                RateLimitConstants.LOGIN_LIMIT_PER_MINUTE_PER_USER,
                RateLimitConstants.WINDOW_SECONDS,
                ResultCode.TOO_MANY_REQUESTS
            );
        }
        if (StringUtils.hasText(deviceId)) {
            rateLimitService.checkRateLimit(
                "rate:login:device:" + deviceId,
                RateLimitConstants.LOGIN_LIMIT_PER_MINUTE_PER_DEVICE,
                RateLimitConstants.WINDOW_SECONDS,
                ResultCode.TOO_MANY_REQUESTS
            );
        }
        assertNotLocked(loginUsername, deviceId);
        if (shouldRequireCaptcha(loginUsername, deviceId)) {
            verifyLoginCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        }

        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, loginUsername)
        );
        if (user == null) {
            recordLoginLog(null, loginUsername, 1, ResultCode.LOGIN_FAILED.getMessage());
            markLoginFailure(loginUsername, deviceId);
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.LOGIN_FAILED.getMessage());
            markLoginFailure(loginUsername, deviceId);
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        clearLoginRisk(loginUsername, deviceId);
        // 检查封禁状态
        if (user.getStatus() == 1) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.USER_BANNED.getMessage());
            throw new BusinessException(ResultCode.USER_BANNED);
        }
        if (blocklistService.isBlocked(BLOCKLIST_TYPE_USER, String.valueOf(user.getId()))) {
            recordLoginLog(user.getId(), user.getUsername(), 1, ResultCode.USER_BANNED.getMessage());
            throw new BusinessException(ResultCode.USER_BANNED);
        }

        try {
            ensureLoginBinding(user);
        } catch (BusinessException e) {
            recordLoginLog(user.getId(), user.getUsername(), 1, e.getMessage());
            throw e;
        }

        // 执行登录
        StpUtil.login(user.getId(), true);
        String refreshToken = issueRefreshToken(user.getId(), deviceId, clientIp);
        bindTokenSession(user, refreshToken);
        recordOnlineToken();

        // 更新登录时间
        user.setLoginDate(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        recordLoginLog(user.getId(), user.getUsername(), 0, "登录成功");
        return buildLoginVO(user, refreshToken);
    }

    @Override
    public LoginCaptchaVO getLoginCaptcha() {
        String clientIp = resolveClientIp();
        rateLimitService.checkRateLimit(
            "rate:login-captcha:ip:" + clientIp,
            RateLimitConstants.LOGIN_CAPTCHA_LIMIT_PER_MINUTE,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS
        );

        String captchaCode = generateLoginCaptchaCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
            LOGIN_CAPTCHA_PREFIX + captchaId,
            captchaCode,
            Duration.ofSeconds(RateLimitConstants.LOGIN_CAPTCHA_TTL_SECONDS)
        );

        LoginCaptchaVO vo = new LoginCaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setChallenge(LOGIN_CAPTCHA_PROMPT);
        vo.setCaptchaImage(buildLoginCaptchaImageDataUrl(captchaCode));
        vo.setExpireSeconds(RateLimitConstants.LOGIN_CAPTCHA_TTL_SECONDS);
        return vo;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        String resolvedRefreshToken = resolveRefreshToken(refreshToken);
        if (!StringUtils.hasText(resolvedRefreshToken)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "refreshToken不能为空");
        }
        String key = buildRefreshTokenKey(resolvedRefreshToken);
        // 原子消费 refresh token，阻断并发重放。
        String payload = redisTemplate.opsForValue().getAndDelete(key);
        if (!StringUtils.hasText(payload)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED, "刷新令牌已过期，请重新登录");
        }
        String[] parts = payload.split(":", 3);
        if (parts.length < 1 || !StringUtils.hasText(parts[0])) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED, "刷新令牌无效，请重新登录");
        }
        Long userId;
        try {
            userId = Long.valueOf(parts[0]);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED, "刷新令牌无效，请重新登录");
        }
        String boundDeviceId = parts.length >= 2 ? parts[1] : null;
        String currentDeviceId = resolveDeviceId();
        if (StringUtils.hasText(boundDeviceId)) {
            if (!StringUtils.hasText(currentDeviceId) || !boundDeviceId.equals(currentDeviceId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "刷新令牌与当前设备不匹配");
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            revokeRefreshToken(userId, resolvedRefreshToken);
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            revokeRefreshToken(userId, resolvedRefreshToken);
            throw new BusinessException(ResultCode.USER_BANNED);
        }
        if (blocklistService.isBlocked(BLOCKLIST_TYPE_USER, String.valueOf(userId))) {
            revokeRefreshToken(userId, resolvedRefreshToken);
            throw new BusinessException(ResultCode.USER_BANNED);
        }
        ensureLoginBinding(user);

        // 轮转 refresh token，避免长期复用同一令牌。
        revokeRefreshToken(userId, resolvedRefreshToken);
        StpUtil.login(userId, true);
        String newRefreshToken = issueRefreshToken(userId, StringUtils.hasText(currentDeviceId) ? currentDeviceId : boundDeviceId, resolveClientIp());
        bindTokenSession(user, newRefreshToken);
        recordOnlineToken();
        return buildLoginVO(user, newRefreshToken);
    }

    @Override
    public void logout() {
        clearOnlineToken();
        revokeCurrentRefreshToken();
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
        PasswordPolicyUtil.validateOrThrow(dto.getNewPassword());

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

        // 修改密码后强制全设备下线并回收全部刷新令牌
        revokeAllRefreshTokensByUser(userId);
        onlineUserService.kickoutByUserId(userId);
    }

    @Override
    public void sendRegisterEmailCode(String email) {
        if (!sysConfigService.isEmailVerificationEnabled()) {
            throw new BusinessException("邮箱验证未开启");
        }
        String normalizedEmail = SensitiveFieldUtil.normalizeEmail(email);
        validateEmailDomain(normalizedEmail);
        String emailHash = SensitiveFieldUtil.hashEmail(normalizedEmail);

        Long emailCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getEmailHash, emailHash)
                    .or()
                    .eq(User::getEduEmailHash, emailHash)
                    .or()
                    .eq(User::getEmail, normalizedEmail)
                    .or()
                    .eq(User::getEduEmail, normalizedEmail))
        );
        if (emailCount > 0) {
            throw new BusinessException("邮箱已被使用");
        }

        String clientIp = resolveClientIp();
        String emailLower = normalizedEmail;
        
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
        variables.put("email", normalizedEmail);
        sysConfigService.sendEmailWithTemplate(normalizedEmail, "verification", variables);
    }

    @Override
    public void sendEmailCode(String eduEmail) {
        String normalizedEduEmail = SensitiveFieldUtil.normalizeEmail(eduEmail);
        // 验证EDU邮箱格式
        validateEmailDomain(normalizedEduEmail);

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
        redisTemplate.opsForValue().set(key, normalizedEduEmail + ":" + code,
            EMAIL_CODE_TTL, java.util.concurrent.TimeUnit.SECONDS);

        // 实际发送邮件
        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("code", code);
        variables.put("expireMinutes", String.valueOf(EMAIL_CODE_TTL / 60));
        variables.put("email", normalizedEduEmail);
        sysConfigService.sendEmailWithTemplate(normalizedEduEmail, "verification", variables);
        
        // 仅在开发环境下打印验证码
        if (isDevProfile()) {
            System.out.println("[DEV] 邮箱验证码: " + code + " -> " + normalizedEduEmail);
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

        String eduEmail = SensitiveFieldUtil.normalizeEmail(parts[0]);
        String savedCode = parts[1];

        if (!savedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }

        // 更新用户验证状态
        User user = userMapper.selectById(userId);
        user.setEduEmail(eduEmail);
        user.setEduEmailHash(SensitiveFieldUtil.hashEmail(eduEmail));
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
        String emailLower = email.toLowerCase(Locale.ROOT);
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

    private void ensureLoginBinding(User user) {
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        if (roleKeys == null || roleKeys.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账号未绑定角色，请联系管理员");
        }
        boolean isSuperAdminRole = roleKeys.contains(SecurityUtil.getSuperAdminRoleKey());
        if (user.getUserType() != null
            && user.getUserType() == 1
            && !isSuperAdminRole) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "管理员账号角色异常，请联系管理员");
        }
        if (isSuperAdminRole) {
            return;
        }

        List<Long> userDeptIds = roleDeptMapper.selectDeptIdsByUserId(user.getId());
        if (userDeptIds == null || userDeptIds.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账号角色未绑定部门，请联系管理员");
        }

        var dept = deptMapper.selectById(userDeptIds.getFirst());
        if (dept == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账号部门不存在，请联系管理员");
        }
        if (dept.getStatus() != null && dept.getStatus() == 1) {
            throw new BusinessException(ResultCode.DEPT_DISABLED);
        }
        if (dept.getDataScope() == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账号部门未配置数据权限，请联系管理员");
        }
        if (dept.getDataScope() < SecurityConstants.DATA_SCOPE_ALL
            || dept.getDataScope() > SecurityConstants.DATA_SCOPE_SELF) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账号部门数据权限无效，请联系管理员");
        }
    }

    private LoginVO buildLoginVO(User user, String refreshToken) {
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setTokenExpiresIn(StpUtil.getTokenTimeout());
        vo.setRefreshToken(refreshToken);
        vo.setRefreshTokenExpiresIn((long) RateLimitConstants.REFRESH_TOKEN_TTL_SECONDS);
        vo.setUserInfo(buildUserInfoVO(user));
        return vo;
    }

    private String normalizeLoginUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return username.trim();
    }

    private String resolveDeviceId() {
        if (request == null) {
            return null;
        }
        for (String header : DEVICE_ID_HEADERS) {
            String normalized = DeviceIdUtil.normalizeOrNull(request.getHeader(header));
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String generateLoginCaptchaCode() {
        StringBuilder builder = new StringBuilder(LOGIN_CAPTCHA_CODE_LENGTH);
        for (int i = 0; i < LOGIN_CAPTCHA_CODE_LENGTH; i++) {
            int index = RandomUtil.randomInt(LOGIN_CAPTCHA_CHARSET.length);
            builder.append(LOGIN_CAPTCHA_CHARSET[index]);
        }
        return builder.toString();
    }

    private String buildLoginCaptchaImageDataUrl(String captchaCode) {
        BufferedImage image = new BufferedImage(
            LOGIN_CAPTCHA_IMAGE_WIDTH,
            LOGIN_CAPTCHA_IMAGE_HEIGHT,
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(randomLightColor());
            graphics.fillRect(0, 0, LOGIN_CAPTCHA_IMAGE_WIDTH, LOGIN_CAPTCHA_IMAGE_HEIGHT);

            for (int i = 0; i < 10; i++) {
                graphics.setColor(randomNoiseColor());
                graphics.drawLine(
                    RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_WIDTH),
                    RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_HEIGHT),
                    RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_WIDTH),
                    RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_HEIGHT)
                );
            }

            for (int i = 0; i < 40; i++) {
                graphics.setColor(randomNoiseColor());
                int x = RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_WIDTH);
                int y = RandomUtil.randomInt(LOGIN_CAPTCHA_IMAGE_HEIGHT);
                graphics.fillRect(x, y, 1, 1);
            }

            graphics.setFont(new Font("SansSerif", Font.BOLD, 30));
            int charSpace = LOGIN_CAPTCHA_IMAGE_WIDTH / (captchaCode.length() + 1);
            for (int i = 0; i < captchaCode.length(); i++) {
                char ch = captchaCode.charAt(i);
                int centerX = charSpace * (i + 1);
                int baseY = (LOGIN_CAPTCHA_IMAGE_HEIGHT / 2) + 12 + RandomUtil.randomInt(-4, 5);
                AffineTransform oldTransform = graphics.getTransform();
                double angle = Math.toRadians(RandomUtil.randomInt(-30, 31));
                graphics.rotate(angle, centerX, baseY);
                graphics.setColor(randomTextColor());
                graphics.drawString(String.valueOf(ch), centerX - 9, baseY);
                graphics.setTransform(oldTransform);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码生成失败");
        }
    }

    private Color randomLightColor() {
        return new Color(
            RandomUtil.randomInt(220, 256),
            RandomUtil.randomInt(220, 256),
            RandomUtil.randomInt(220, 256)
        );
    }

    private Color randomNoiseColor() {
        return new Color(
            RandomUtil.randomInt(120, 220),
            RandomUtil.randomInt(120, 220),
            RandomUtil.randomInt(120, 220)
        );
    }

    private Color randomTextColor() {
        return new Color(
            RandomUtil.randomInt(20, 150),
            RandomUtil.randomInt(20, 150),
            RandomUtil.randomInt(20, 150)
        );
    }

    private boolean shouldRequireCaptcha(String normalizedUsername, String deviceId) {
        long userFailCount = StringUtils.hasText(normalizedUsername)
            ? getLongValue(LOGIN_FAIL_USER_PREFIX + normalizedUsername)
            : 0L;
        long deviceFailCount = StringUtils.hasText(deviceId)
            ? getLongValue(LOGIN_FAIL_DEVICE_PREFIX + deviceId)
            : 0L;
        return userFailCount >= RateLimitConstants.LOGIN_FAIL_CAPTCHA_THRESHOLD
            || deviceFailCount >= RateLimitConstants.LOGIN_FAIL_CAPTCHA_THRESHOLD;
    }

    private void verifyLoginCaptcha(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "登录失败次数较多，请先完成验证码");
        }
        String key = LOGIN_CAPTCHA_PREFIX + captchaId.trim();
        String expected = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(expected)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误");
        }
        redisTemplate.delete(key);
    }

    private void markLoginFailure(String normalizedUsername, String deviceId) {
        if (StringUtils.hasText(normalizedUsername)) {
            increaseFailureCounter(LOGIN_FAIL_USER_PREFIX + normalizedUsername, LOGIN_LOCK_USER_PREFIX + normalizedUsername);
        }
        if (StringUtils.hasText(deviceId)) {
            increaseFailureCounter(LOGIN_FAIL_DEVICE_PREFIX + deviceId, LOGIN_LOCK_DEVICE_PREFIX + deviceId);
        }
    }

    private void increaseFailureCounter(String counterKey, String lockKey) {
        if (!StringUtils.hasText(counterKey)) {
            return;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(counterKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(counterKey, Duration.ofSeconds(RateLimitConstants.LOGIN_FAIL_WINDOW_SECONDS));
            }
            if (count != null && count >= RateLimitConstants.LOGIN_FAIL_LOCK_THRESHOLD && StringUtils.hasText(lockKey)) {
                redisTemplate.opsForValue().set(lockKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(RateLimitConstants.LOGIN_LOCK_SECONDS));
            }
        } catch (Exception e) {
            // Redis 风控降级不影响主流程
        }
    }

    private void clearLoginRisk(String normalizedUsername, String deviceId) {
        if (StringUtils.hasText(normalizedUsername)) {
            redisTemplate.delete(LOGIN_FAIL_USER_PREFIX + normalizedUsername);
            redisTemplate.delete(LOGIN_LOCK_USER_PREFIX + normalizedUsername);
        }
        if (StringUtils.hasText(deviceId)) {
            redisTemplate.delete(LOGIN_FAIL_DEVICE_PREFIX + deviceId);
            redisTemplate.delete(LOGIN_LOCK_DEVICE_PREFIX + deviceId);
        }
    }

    private void assertNotLocked(String normalizedUsername, String deviceId) {
        if (StringUtils.hasText(normalizedUsername)) {
            String userLockKey = LOGIN_LOCK_USER_PREFIX + normalizedUsername;
            long userLockTtl = getLockTtlSeconds(userLockKey);
            if (userLockTtl > 0) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "账号已临时锁定，请" + userLockTtl + "秒后再试");
            }
        }
        if (StringUtils.hasText(deviceId)) {
            String deviceLockKey = LOGIN_LOCK_DEVICE_PREFIX + deviceId;
            long deviceLockTtl = getLockTtlSeconds(deviceLockKey);
            if (deviceLockTtl > 0) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "设备登录已临时锁定，请" + deviceLockTtl + "秒后再试");
            }
        }
    }

    private long getLongValue(String key) {
        if (!StringUtils.hasText(key)) {
            return 0L;
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return 0L;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private long getLockTtlSeconds(String lockKey) {
        if (!StringUtils.hasText(lockKey)) {
            return 0L;
        }
        try {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            return ttl == null ? 0L : Math.max(0L, ttl);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String issueRefreshToken(Long userId, String deviceId, String ip) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String safeDevice = StringUtils.hasText(deviceId) ? deviceId.trim() : "";
        String safeIp = StringUtils.hasText(ip) ? ip.trim() : "";
        String payload = userId + ":" + safeDevice + ":" + safeIp;
        String refreshKey = buildRefreshTokenKey(token);
        redisTemplate.opsForValue().set(
            refreshKey,
            payload,
            Duration.ofSeconds(RateLimitConstants.REFRESH_TOKEN_TTL_SECONDS)
        );
        if (userId != null) {
            String userIndexKey = buildRefreshUserIndexKey(userId);
            redisTemplate.opsForSet().add(userIndexKey, token);
            redisTemplate.expire(userIndexKey, Duration.ofSeconds(RateLimitConstants.REFRESH_TOKEN_TTL_SECONDS));
        }
        return token;
    }

    private String resolveRefreshToken(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            return normalizeRefreshTokenOrThrow(refreshToken);
        }
        if (request == null) {
            return null;
        }
        var cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (var cookie : cookies) {
            if (cookie == null) {
                continue;
            }
            if (SecurityConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())
                && StringUtils.hasText(cookie.getValue())) {
                return normalizeRefreshTokenOrThrow(cookie.getValue());
            }
        }
        return null;
    }

    private void validateLoginPayload(String loginUsername, String password) {
        if (StringUtils.hasText(loginUsername) && loginUsername.length() > LOGIN_USERNAME_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名长度不能超过" + LOGIN_USERNAME_MAX_LENGTH + "个字符");
        }
        if (StringUtils.hasText(password) && password.length() > LOGIN_PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码长度不能超过" + LOGIN_PASSWORD_MAX_LENGTH + "个字符");
        }
    }

    private String normalizeRefreshTokenOrThrow(String token) {
        String normalized = token == null ? null : token.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "refreshToken不能为空");
        }
        if (normalized.length() > REFRESH_TOKEN_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "refreshToken长度不合法");
        }
        if (!REFRESH_TOKEN_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "refreshToken格式不合法");
        }
        return normalized;
    }

    private void revokeCurrentRefreshToken() {
        try {
            var tokenSession = StpUtil.getTokenSession();
            Object refreshToken = tokenSession.get(SecurityConstants.TOKEN_SESSION_REFRESH_TOKEN);
            if (refreshToken instanceof String token && StringUtils.hasText(token)) {
                Long userId = null;
                Object loginId = StpUtil.getLoginIdDefaultNull();
                if (loginId != null) {
                    try {
                        userId = Long.valueOf(loginId.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                revokeRefreshToken(userId, token);
            }
        } catch (Exception ignore) {
            // 刷新令牌清理失败不影响登出
        }
    }

    private void revokeRefreshToken(Long userId, String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        String normalized = refreshToken.trim();
        redisTemplate.delete(buildRefreshTokenKey(normalized));
        if (userId != null) {
            redisTemplate.opsForSet().remove(buildRefreshUserIndexKey(userId), normalized);
        }
    }

    private void revokeAllRefreshTokensByUser(Long userId) {
        if (userId == null) {
            return;
        }
        String indexKey = buildRefreshUserIndexKey(userId);
        try {
            java.util.Set<String> tokens = redisTemplate.opsForSet().members(indexKey);
            if (tokens != null && !tokens.isEmpty()) {
                java.util.List<String> keys = tokens.stream()
                    .filter(StringUtils::hasText)
                    .map(this::buildRefreshTokenKey)
                    .toList();
                if (!keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
            redisTemplate.delete(indexKey);
        } catch (Exception ignored) {
            // 回收失败不阻断主流程
        }
    }

    private String buildRefreshTokenKey(String token) {
        return LOGIN_REFRESH_TOKEN_PREFIX + token;
    }

    private String buildRefreshUserIndexKey(Long userId) {
        return LOGIN_REFRESH_USER_INDEX_PREFIX + userId;
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

    private void bindTokenSession(User user, String refreshToken) {
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
        tokenSession.set(SecurityConstants.TOKEN_SESSION_REFRESH_TOKEN, refreshToken);
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
        assertNoPendingVerification(userId);
        Long fileId = resolveOwnedIdCardFileId(userId, dto.getImageUrl());

        // 如果提供了学号，检查唯一性
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            assertStudentIdAvailable(dto.getStudentId(), userId);
        }

        // 创建审核记录
        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setImageUrl(String.valueOf(fileId));
        verification.setVerifyMethod("ID_CARD");
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            String studentId = SensitiveFieldUtil.normalizeStudentId(dto.getStudentId());
            verification.setStudentId(studentId);
            verification.setStudentIdHash(SensitiveFieldUtil.hashStudentId(studentId));
        }
        verification.setStatus(0); // 待审核
        verificationMapper.insert(verification);

        updateUserVerifyStatus(userId, 1, "ID_CARD");
        return verification.getId();
    }

    @Override
    @Transactional
    public Long submitStudentId(SubmitStudentIdDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        assertNoPendingVerification(userId);

        assertStudentIdAvailable(dto.getStudentId(), userId);

        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setVerifyMethod("ID_LIST");
        String studentId = SensitiveFieldUtil.normalizeStudentId(dto.getStudentId());
        verification.setStudentId(studentId);
        verification.setStudentIdHash(SensitiveFieldUtil.hashStudentId(studentId));
        verification.setStatus(0);
        verificationMapper.insert(verification);

        updateUserVerifyStatus(userId, 1, "ID_LIST");
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

        updateUserVerifyStatus(userId, 0, null);
    }

    private void assertNoPendingVerification(Long userId) {
        Long pendingCount = verificationMapper.selectCount(
            new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getUserId, userId)
                .eq(IdentityVerification::getStatus, 0)
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的申请，请耐心等待");
        }
    }

    private void updateUserVerifyStatus(Long userId, int verifyStatus, String verifyMethod) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setVerifyStatus(verifyStatus);
            user.setVerifyMethod(verifyMethod);
            userMapper.updateById(user);
        }
    }

    private void assertStudentIdAvailable(String studentId, Long userId) {
        String studentIdHash = SensitiveFieldUtil.hashStudentId(studentId);
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

    private Long resolveOwnedIdCardFileId(Long userId, String imageRef) {
        if (!StringUtils.hasText(imageRef)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学生证图片不能为空");
        }
        Long fileId;
        try {
            fileId = Long.valueOf(imageRef.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学生证图片参数不合法");
        }

        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学生证图片不存在");
        }
        if (record.getUserId() == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权使用该学生证图片");
        }
        if (!TARGET_TYPE_ID_CARD.equalsIgnoreCase(record.getTargetType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学生证图片类型不合法");
        }
        if (!FileVisibility.PRIVATE.getCode().equalsIgnoreCase(record.getVisibility())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学生证图片必须为私有文件");
        }
        Integer auditStatus = record.getAuditStatus();
        if (auditStatus == null || auditStatus != FileAuditStatus.PASSED.getCode()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "学生证图片审核未通过");
        }
        return fileId;
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
