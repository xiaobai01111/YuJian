package com.campus.wall.service.security;

import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    @Mock
    private SysRoleDeptMapper roleDeptMapper;

    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    private DataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        dataScopeService = new DataScopeService(roleDeptMapper, deptMapper, roleMapper, userMapper);
    }

    @Test
    void resolveScope_selfScopeEnablesAllowSelf() {
        long userId = 1L;
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_SELF);

        when(roleDeptMapper.selectDeptIdsByUserId(userId)).thenReturn(List.of(10L));
        when(deptMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(dept));

        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        assertThat(scope.isAllowSelf()).isTrue();
        assertThat(scope.isAllowAll()).isFalse();
        assertThat(scope.getScopedDeptIds()).isEmpty();
    }

    @Test
    void resolveScope_deptScopeDoesNotEnableAllowSelf() {
        long userId = 1L;
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);

        when(roleDeptMapper.selectDeptIdsByUserId(userId)).thenReturn(List.of(20L));
        when(deptMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(dept));

        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        assertThat(scope.isAllowSelf()).isFalse();
        assertThat(scope.isAllowAll()).isFalse();
        assertThat(scope.getScopedDeptIds()).containsExactly(20L);
    }
}
