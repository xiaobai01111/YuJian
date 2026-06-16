package com.campus.wall.service.system;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.system.SysAuthRule;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysAuthRuleMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.service.system.impl.AuthRuleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRuleServiceImplTest {

    @Mock
    private SysAuthRuleMapper authRuleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private AuthRuleServiceImpl authRuleService;

    @Test
    void applyRules_matchEmailDomain_assignsRoles() {
        SysAuthRule rule = new SysAuthRule();
        rule.setEnabled(true);
        rule.setTriggerType("VERIFY");
        rule.setMatchType("EMAIL_DOMAIN");
        rule.setMatchValue("school.edu");
        rule.setRoleIds(List.of(1L, 2L));

        when(authRuleMapper.selectList(any())).thenReturn(List.of(rule));

        User user = new User();
        user.setId(10L);
        user.setEmail("user@school.edu");

        authRuleService.applyRules(user, "VERIFY", null);

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void applyRules_noMatch_skipsAssign() {
        SysAuthRule rule = new SysAuthRule();
        rule.setEnabled(true);
        rule.setTriggerType("VERIFY");
        rule.setMatchType("STUDENT_ID_PREFIX");
        rule.setMatchValue("2024");
        rule.setRoleIds(List.of(1L));

        when(authRuleMapper.selectList(any())).thenReturn(List.of(rule));

        User user = new User();
        user.setId(11L);
        user.setStudentId("2023ABC");

        authRuleService.applyRules(user, "VERIFY", null);

        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void updateStatus_defaultsToEnabled() {
        SysAuthRule rule = new SysAuthRule();
        rule.setId(5L);
        rule.setEnabled(false);

        when(authRuleMapper.selectById(5L)).thenReturn(rule);

        authRuleService.updateStatus(5L, null);

        ArgumentCaptor<SysAuthRule> captor = ArgumentCaptor.forClass(SysAuthRule.class);
        verify(authRuleMapper).updateById(captor.capture());
        assertThat(captor.getValue().getEnabled()).isTrue();
    }

    @Test
    void updateStatus_missingRule_throws() {
        when(authRuleMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> authRuleService.updateStatus(9L, true))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode())
            .hasMessage("规则不存在");
    }
}
