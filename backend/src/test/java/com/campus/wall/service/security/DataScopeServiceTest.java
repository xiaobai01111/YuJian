package com.campus.wall.service.security;

import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    @Mock
    private SysRoleDeptMapper roleDeptMapper;

    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private UserMapper userMapper;

    private DataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        dataScopeService = new DataScopeService(roleDeptMapper, deptMapper, userMapper);
    }

    @Test
    void resolveAllowedUserIds_selfScopeIncludesSelf() {
        long userId = 1L;
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_SELF);

        when(roleDeptMapper.selectDeptIdsByUserId(userId)).thenReturn(List.of(10L));
        when(deptMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(dept));

        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        List<Long> allowed = dataScopeService.resolveAllowedUserIds(scope, userId);

        assertThat(allowed).containsExactly(userId);
    }

    @Test
    void resolveAllowedUserIds_deptScopeDoesNotAutoIncludeSelf() {
        long userId = 1L;
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);

        User deptUser = new User();
        deptUser.setId(2L);
        deptUser.setDeptId(20L);

        when(roleDeptMapper.selectDeptIdsByUserId(userId)).thenReturn(List.of(20L));
        when(deptMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(dept));
        when(userMapper.selectList(any())).thenReturn(List.of(deptUser));

        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        List<Long> allowed = dataScopeService.resolveAllowedUserIds(scope, userId);

        assertThat(allowed).containsExactly(2L);
    }
}
