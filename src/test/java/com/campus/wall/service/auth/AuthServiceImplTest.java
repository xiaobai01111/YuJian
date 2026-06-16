package com.campus.wall.service.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.dto.auth.LoginDTO;
import com.campus.wall.dto.auth.RegisterDTO;
import com.campus.wall.dto.auth.SubmitStudentIdDTO;
import com.campus.wall.dto.auth.UpdatePasswordDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.auth.impl.AuthServiceImpl;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.system.BlocklistService;
import com.campus.wall.service.system.LoginLogService;
import com.campus.wall.service.system.OnlineUserService;
import com.campus.wall.service.system.SysConfigService;
import com.campus.wall.support.SaTokenTestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private Environment environment;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleDeptMapper roleDeptMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private IdentityVerificationMapper verificationMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AuthRuleService authRuleService;
    @Mock
    private BlocklistService blocklistService;
    @Mock
    private LoginLogService loginLogService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void login_wrongPassword_fails() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("user");
        dto.setPassword("bad");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.LOGIN_FAILED.getCode());
    }

    @Test
    void sendRegisterEmailCode_invalidDomain_throws() {
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(true);
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));

        assertThatThrownBy(() -> authService.sendRegisterEmailCode("user@qq.com"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("请使用允许的邮箱域名");
    }

    @Test
    void confirmEmailCode_wrongCode_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(2L);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("campus:verify:email:2")).thenReturn("user@edu.cn:123456");

        assertThatThrownBy(() -> authService.confirmEmailCode("000000"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("验证码错误");

        verify(userMapper, never()).updateById(any());
        verify(authRuleService, never()).applyRules(any(), any(), any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void register_passwordMismatch_throws() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("u1");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@2");
        dto.setEmail("u1@edu.cn");

        assertThatThrownBy(() -> authService.register(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("两次密码输入不一致");
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("u1");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@1");
        dto.setEmail("u1@edu.cn");
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("用户名已存在");
    }

    @Test
    void register_emailVerificationMissingCode_throws() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("u2");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@1");
        dto.setEmail("u2@edu.cn");
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));
        when(userMapper.selectCount(any())).thenReturn(0L, 0L);
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(true);

        assertThatThrownBy(() -> authService.register(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("请填写邮箱验证码");
    }

    @Test
    void register_emailVerificationWrongCode_throws() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("u3");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@1");
        dto.setEmail("u3@edu.cn");
        dto.setEmailCode("000000");
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));
        when(userMapper.selectCount(any())).thenReturn(0L, 0L);
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(true);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("campus:register:email:u3@edu.cn")).thenReturn("123456");

        assertThatThrownBy(() -> authService.register(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("验证码错误");
    }

    @Test
    void register_success_appliesRules() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("u4");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@1");
        dto.setEmail("u4@edu.cn");
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));
        when(userMapper.selectCount(any())).thenReturn(0L, 0L);
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(false);
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Long userId = authService.register(dto);

        assertThat(userId).isEqualTo(100L);
        verify(authRuleService).applyRules(any(User.class), eq("REGISTER"), isNull());
    }

    @Test
    void login_userNotFound_throws() {
        when(userMapper.selectOne(any())).thenReturn(null);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("missing");
        dto.setPassword("x");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.LOGIN_FAILED.getCode());
    }

    @Test
    void login_usernameTooLong_throwsBadRequest() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("a".repeat(51));
        dto.setPassword("x");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessageContaining("用户名长度不能超过50个字符");
    }

    @Test
    void login_riskKeyAndLookupKeepUsernameCaseConsistent() {
        when(userMapper.selectOne(any())).thenReturn(null);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("  UserA  ");
        dto.setPassword("x");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.LOGIN_FAILED.getCode());

        verify(rateLimitService).checkRateLimit(
            eq("rate:login:user:UserA"),
            eq(RateLimitConstants.LOGIN_LIMIT_PER_MINUTE_PER_USER),
            eq(RateLimitConstants.WINDOW_SECONDS),
            eq(ResultCode.TOO_MANY_REQUESTS)
        );
        verify(valueOperations).increment("campus:login:fail:user:UserA");
        verify(loginLogService).recordLogin(
            isNull(),
            eq("UserA"),
            eq(1),
            eq(ResultCode.LOGIN_FAILED.getMessage()),
            any(),
            any()
        );
    }

    @Test
    void getLoginCaptcha_appliesIpRateLimit() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        var captcha = authService.getLoginCaptcha();

        verify(rateLimitService).checkRateLimit(
            eq("rate:login-captcha:ip:127.0.0.1"),
            eq(RateLimitConstants.LOGIN_CAPTCHA_LIMIT_PER_MINUTE),
            eq(RateLimitConstants.WINDOW_SECONDS),
            eq(ResultCode.TOO_MANY_REQUESTS)
        );
        assertThat(captcha.getCaptchaId()).isNotBlank();
        assertThat(captcha.getCaptchaImage()).startsWith("data:image/png;base64,");
        assertThat(captcha.getChallenge()).isEqualTo("请输入图形验证码");
        assertThat(captcha.getExpireSeconds()).isEqualTo(RateLimitConstants.LOGIN_CAPTCHA_TTL_SECONDS);
        verify(valueOperations).set(
            org.mockito.ArgumentMatchers.startsWith("campus:login:captcha:"),
            anyString(),
            eq(Duration.ofSeconds(RateLimitConstants.LOGIN_CAPTCHA_TTL_SECONDS))
        );
    }

    @Test
    void refreshToken_tooLong_throwsBadRequest() {
        String tooLongToken = "a".repeat(129);

        assertThatThrownBy(() -> authService.refreshToken(tooLongToken))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessageContaining("refreshToken长度不合法");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void login_userBanned_throws() {
        User user = new User();
        user.setId(10L);
        user.setUsername("u10");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("u10");
        dto.setPassword("Password@1");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.USER_BANNED.getCode());
    }

    @Test
    void login_missingRole_throws() {
        User user = new User();
        user.setId(11L);
        user.setUsername("u11");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRoleKeysByUserId(11L)).thenReturn(List.of());
        LoginDTO dto = new LoginDTO();
        dto.setUsername("u11");
        dto.setPassword("Password@1");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("账号未绑定角色，请联系管理员");
    }

    @Test
    void login_adminTypeWithoutSuperAdminRole_throws() {
        User user = new User();
        user.setId(111L);
        user.setUsername("u111");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));
        user.setStatus(0);
        user.setUserType(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRoleKeysByUserId(111L)).thenReturn(List.of("student"));

        LoginDTO dto = new LoginDTO();
        dto.setUsername("u111");
        dto.setPassword("Password@1");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员账号角色异常，请联系管理员");
    }

    @Test
    void login_deptDisabled_throws() {
        User user = new User();
        user.setId(12L);
        user.setUsername("u12");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRoleKeysByUserId(12L)).thenReturn(List.of("student"));
        when(roleDeptMapper.selectDeptIdsByUserId(12L)).thenReturn(List.of(2L));
        SysDept dept = new SysDept();
        dept.setId(2L);
        dept.setStatus(1);
        when(deptMapper.selectById(2L)).thenReturn(dept);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("u12");
        dto.setPassword("Password@1");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.DEPT_DISABLED.getCode());
    }

    @Test
    void login_success_returnsTokenAndUserInfo() {
        User user = new User();
        user.setId(13L);
        user.setUsername("u13");
        user.setNickname("N13");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Password@1"));
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectRoleKeysByUserId(13L)).thenReturn(List.of("student"));
        when(roleDeptMapper.selectDeptIdsByUserId(13L)).thenReturn(List.of(1L));
        SysDept dept = new SysDept();
        dept.setId(1L);
        dept.setStatus(0);
        dept.setDataScope(4);
        when(deptMapper.selectById(1L)).thenReturn(dept);
        when(menuMapper.selectPermsByUserId(13L)).thenReturn(List.of("p:a"));
        LoginDTO dto = new LoginDTO();
        dto.setUsername("u13");
        dto.setPassword("Password@1");

        var vo = authService.login(dto);

        assertThat(vo.getToken()).isNotBlank();
        assertThat(vo.getUserInfo().getUsername()).isEqualTo("u13");
        assertThat(vo.getUserInfo().getPermissions()).contains("p:a");
        verify(userMapper).updateById(user);
    }

    @Test
    void updatePassword_oldPasswordWrong_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(21L);
        User user = new User();
        user.setId(21L);
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("old-pass"));
        when(userMapper.selectById(21L)).thenReturn(user);
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword("wrong");
        dto.setNewPassword("New@pass1");
        dto.setConfirmPassword("New@pass1");

        assertThatThrownBy(() -> authService.updatePassword(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("原密码错误");
    }

    @Test
    void updatePassword_success_updatesAndLogout() {
        SaTokenTestContext.bind();
        StpUtil.login(22L);
        User user = new User();
        user.setId(22L);
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("old-pass"));
        when(userMapper.selectById(22L)).thenReturn(user);
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword("old-pass");
        dto.setNewPassword("New@pass1");
        dto.setConfirmPassword("New@pass1");

        authService.updatePassword(dto);

        verify(userMapper).updateById(user);
        verify(onlineUserService).kickoutByUserId(22L);
    }

    @Test
    void sendRegisterEmailCode_disabled_throws() {
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(false);

        assertThatThrownBy(() -> authService.sendRegisterEmailCode("user@edu.cn"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("邮箱验证未开启");
    }

    @Test
    void sendRegisterEmailCode_cooldownHit_throws() {
        when(sysConfigService.isEmailVerificationEnabled()).thenReturn(true);
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));
        when(userMapper.selectCount(any())).thenReturn(0L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq("rate:register-email:cooldown:user@edu.cn"), eq("1"), any(java.time.Duration.class)))
            .thenReturn(false);

        assertThatThrownBy(() -> authService.sendRegisterEmailCode("user@edu.cn"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("请稍后再试");
    }

    @Test
    void sendEmailCode_success_sendsTemplate() {
        SaTokenTestContext.bind();
        StpUtil.login(30L);
        when(sysConfigService.getEmailAllowedDomains()).thenReturn(List.of("edu.cn"));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        authService.sendEmailCode("u30@edu.cn");

        verify(ops).set(eq("campus:verify:email:30"), anyString(), eq(300L), eq(java.util.concurrent.TimeUnit.SECONDS));
        verify(sysConfigService).sendEmailWithTemplate(eq("u30@edu.cn"), eq("verification"), any());
    }

    @Test
    void confirmEmailCode_expired_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(31L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("campus:verify:email:31")).thenReturn(null);

        assertThatThrownBy(() -> authService.confirmEmailCode("123456"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("验证码已过期，请重新获取");
    }

    @Test
    void confirmEmailCode_invalidFormat_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(32L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("campus:verify:email:32")).thenReturn("bad-format");

        assertThatThrownBy(() -> authService.confirmEmailCode("123456"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("验证码无效");
    }

    @Test
    void submitStudentId_pendingExists_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(40L);
        when(verificationMapper.selectCount(any())).thenReturn(1L);
        SubmitStudentIdDTO dto = new SubmitStudentIdDTO();
        dto.setStudentId("20260001");

        assertThatThrownBy(() -> authService.submitStudentId(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("您已有待审核的申请，请耐心等待");
    }

    @Test
    void submitStudentId_success_insertsAndUpdatesStatus() {
        SaTokenTestContext.bind();
        StpUtil.login(41L);
        when(verificationMapper.selectCount(any())).thenReturn(0L, 0L);
        User user = new User();
        user.setId(41L);
        when(userMapper.selectById(41L)).thenReturn(user);
        doAnswer(invocation -> {
            IdentityVerification v = invocation.getArgument(0);
            v.setId(900L);
            return 1;
        }).when(verificationMapper).insert(any(IdentityVerification.class));
        SubmitStudentIdDTO dto = new SubmitStudentIdDTO();
        dto.setStudentId("20260002");

        Long id = authService.submitStudentId(dto);

        assertThat(id).isEqualTo(900L);
        verify(userMapper).updateById(user);
        assertThat(user.getVerifyStatus()).isEqualTo(1);
        assertThat(user.getVerifyMethod()).isEqualTo("ID_LIST");
    }

    @Test
    void cancelVerification_missingPending_throws() {
        SaTokenTestContext.bind();
        StpUtil.login(42L);
        when(verificationMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.cancelVerification())
            .isInstanceOf(BusinessException.class)
            .hasMessage("没有待审核的认证申请");
    }

    @Test
    void getAdminContact_withAdmin_returnsInfo() {
        SysRole role = new SysRole();
        role.setId(1L);
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(userRoleMapper.selectUserIdsByRoleId(1L)).thenReturn(List.of(99L));
        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@edu.cn");
        admin.setPhone("13812345678");
        when(userMapper.selectById(99L)).thenReturn(admin);

        var vo = authService.getAdminContact();

        assertThat(vo.getEmail()).isEqualTo("admin@edu.cn");
        assertThat(vo.getPhone()).isEqualTo("13812345678");
    }
}
