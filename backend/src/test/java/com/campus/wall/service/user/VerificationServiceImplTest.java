package com.campus.wall.service.user;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.VerificationHandleDTO;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.user.impl.VerificationServiceImpl;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private IdentityVerificationMapper verificationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthRuleService authRuleService;
    @Mock
    private FileAccessService fileAccessService;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(8L);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void handleVerification_alreadyProcessed_throws() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(1L);
        verification.setStatus(1);

        when(verificationMapper.selectById(1L)).thenReturn(verification);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(1);

        assertThatThrownBy(() -> verificationService.handleVerification(1L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该申请已处理");
    }

    @Test
    void handleVerification_rejectWithoutReason_throws() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(2L);
        verification.setStatus(0);
        verification.setUserId(10L);

        when(verificationMapper.selectById(2L)).thenReturn(verification);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(2);

        assertThatThrownBy(() -> verificationService.handleVerification(2L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("拒绝时必须填写原因");
    }

    @Test
    void handleVerification_approveMissingUser_throws() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(3L);
        verification.setStatus(0);
        verification.setUserId(99L);

        when(verificationMapper.selectById(3L)).thenReturn(verification);
        when(userMapper.selectById(99L)).thenReturn(null);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(1);

        assertThatThrownBy(() -> verificationService.handleVerification(3L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode())
            .hasMessage("用户不存在");
    }

    @Test
    void handleVerification_concurrentUpdate_throws() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(4L);
        verification.setStatus(0);
        verification.setUserId(11L);

        when(verificationMapper.selectById(4L)).thenReturn(verification);
        when(verificationMapper.update(eq(null), any())).thenReturn(0);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(2);
        dto.setRejectReason("invalid");

        assertThatThrownBy(() -> verificationService.handleVerification(4L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该申请已处理");
    }

    @Test
    void handleVerification_approveSuccess_updatesUser() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(5L);
        verification.setStatus(0);
        verification.setUserId(12L);
        verification.setVerifyMethod("ID_CARD");

        User user = new User();
        user.setId(12L);

        when(verificationMapper.selectById(5L)).thenReturn(verification);
        when(userMapper.selectById(12L)).thenReturn(user);
        when(verificationMapper.update(eq(null), any())).thenReturn(1);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(1);

        verificationService.handleVerification(5L, dto);

        verify(userMapper).updateById(user);
        verify(authRuleService).applyRules(user, "VERIFY", "ID_CARD");
    }
}
