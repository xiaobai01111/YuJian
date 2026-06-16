package com.campus.wall.service.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.VerificationHandleDTO;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.service.user.impl.VerificationServiceImpl;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, IdentityVerification.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        SaTokenTestContext.bind();
        StpUtil.login(8L);
        lenient().when(dataScopeService.canAccessUser(eq(8L), anyLong())).thenReturn(true);
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

    @Test
    void queryVerifications_selfScope_filtersByCurrentUser() {
        when(dataScopeService.resolveScope(8L)).thenReturn(DataScopeService.DataScope.selfOnly());
        when(dataScopeService.buildUserScopeExistsSql(any(), eq("identity_verifications.user_id")))
            .thenReturn(null);
        when(verificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<IdentityVerification> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getSqlSegment()).containsIgnoringCase("user_id");
            Page<IdentityVerification> result = new Page<>(1, 20);
            result.setTotal(0);
            result.setRecords(List.of());
            return result;
        });

        verificationService.queryVerifications(0, 1, 20);

        verify(dataScopeService).resolveScope(8L);
        verify(dataScopeService).buildUserScopeExistsSql(any(), eq("identity_verifications.user_id"));
    }

    @Test
    void getVerificationDetail_outOfScope_throwsForbidden() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(6L);
        verification.setUserId(66L);
        when(verificationMapper.selectById(6L)).thenReturn(verification);
        when(dataScopeService.canAccessUser(8L, 66L)).thenReturn(false);

        assertThatThrownBy(() -> verificationService.getVerificationDetail(6L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("无权查看该审核记录");
    }

    @Test
    void handleVerification_outOfScope_throwsForbidden() {
        IdentityVerification verification = new IdentityVerification();
        verification.setId(7L);
        verification.setStatus(0);
        verification.setUserId(77L);
        when(verificationMapper.selectById(7L)).thenReturn(verification);
        when(dataScopeService.canAccessUser(8L, 77L)).thenReturn(false);

        VerificationHandleDTO dto = new VerificationHandleDTO();
        dto.setStatus(2);
        dto.setRejectReason("invalid");

        assertThatThrownBy(() -> verificationService.handleVerification(7L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("无权处理该审核记录");
        verify(verificationMapper, never()).update(eq(null), any());
    }
}
