package com.campus.wall.service.system;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleDept;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.impl.RoleServiceImpl;
import com.campus.wall.service.user.UserService;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.vo.system.RoleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRoleDeptMapper sysRoleDeptMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserService userService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private OperLogService operLogService;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(99L);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void createRole_duplicateKey_throws() {
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> roleService.createRole("role", "role_key", 0, 1, null, List.of()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("角色标识已存在");
    }

    @Test
    void createRole_assignMenusWithoutPermission_throws() {
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:assign")).thenReturn(false);

        assertThatThrownBy(() -> roleService.createRole("role", "role_key", 0, 1, null, List.of(1L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("缺少角色授权权限");
    }

    @Test
    void assignDepts_outOfScope_throws() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleKey("role_key");

        when(sysRoleMapper.selectById(2L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:assign")).thenReturn(true);
        when(dataScopeService.resolveScope(99L)).thenReturn(DataScopeService.DataScope.selfOnly());

        assertThatThrownBy(() -> roleService.assignDepts(2L, List.of(10L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能分配超出当前账号数据权限范围的部门");
    }

    @Test
    void deleteRole_missingRole_doesNotThrow() {
        when(sysRoleMapper.selectById(3L)).thenReturn(null);
        when(sysUserRoleMapper.selectUserIdsByRoleId(3L)).thenReturn(List.of());

        roleService.deleteRole(3L, false, "cleanup");

        verify(sysRoleMapper).deleteById(3L);
    }

    @Test
    void getRoleById_missing_throws() {
        when(sysRoleMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> roleService.getRoleById(10L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("角色不存在");
    }

    @Test
    void assignMenus_adminRole_throws() {
        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleKey(SecurityUtil.getSuperAdminRoleKey());
        when(sysRoleMapper.selectById(1L)).thenReturn(adminRole);

        assertThatThrownBy(() -> roleService.assignMenus(1L, List.of(1L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员角色不允许修改权限");
    }

    @Test
    void assignMenus_overMenuScope_throws() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleKey("role_key");
        SysRole operatorRole = new SysRole();
        operatorRole.setId(99L);

        when(sysRoleMapper.selectById(2L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:assign")).thenReturn(true);
        when(sysRoleMapper.selectRolesByUserId(99L)).thenReturn(List.of(operatorRole));
        when(sysMenuMapper.selectMenuIdsByRoleId(99L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> roleService.assignMenus(2L, List.of(2L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能授予超出当前账号权限上限的菜单");
    }

    @Test
    void assignMenus_success_replacesAndLogs() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleKey("role_key");
        SysRole operatorRole = new SysRole();
        operatorRole.setId(99L);

        when(sysRoleMapper.selectById(2L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:assign")).thenReturn(true);
        when(sysRoleMapper.selectRolesByUserId(99L)).thenReturn(List.of(operatorRole));
        when(sysMenuMapper.selectMenuIdsByRoleId(99L)).thenReturn(List.of(1L, 2L));
        when(sysMenuMapper.selectMenuIdsByRoleId(2L)).thenReturn(List.of(2L));
        when(sysUserRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(5L));
        when(sysRoleDeptMapper.selectDeptIdsByRoleId(2L)).thenReturn(List.of());

        var vo = roleService.assignMenus(2L, List.of(1L));

        assertThat(vo.getMenuIds()).containsExactly(1L);
        verify(sysRoleMenuMapper).deleteByRoleId(2L);
        verify(sysRoleMenuMapper).insert(any(SysRoleMenu.class));
    }

    @Test
    void updateRoleStatus_adminRole_throws() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey(SecurityUtil.getSuperAdminRoleKey());
        when(sysRoleMapper.selectById(1L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.updateRoleStatus(1L, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员角色状态不允许修改");
    }

    @Test
    void updateRoleStatus_missingPermission_throws() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleKey("role_key");
        role.setStatus(0);
        when(sysRoleMapper.selectById(2L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:disable")).thenReturn(false);

        assertThatThrownBy(() -> roleService.updateRoleStatus(2L, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("缺少停用角色权限");
    }

    @Test
    void updateRoleStatus_disable_successUpdatesAndLogs() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleKey("role_key");
        role.setStatus(0);
        when(sysRoleMapper.selectById(2L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of());
        when(permissionService.hasPermission(99L, "system:role:disable")).thenReturn(true);
        when(sysUserRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(5L));
        when(sysRoleDeptMapper.selectDeptIdsByRoleId(2L)).thenReturn(List.of());

        roleService.updateRoleStatus(2L, 1);

        verify(sysRoleMapper).updateById(role);
        assertThat(role.getStatus()).isEqualTo(1);
    }

    @Test
    void assignDepts_adminRole_throws() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey(SecurityUtil.getSuperAdminRoleKey());
        when(sysRoleMapper.selectById(1L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.assignDepts(1L, List.of(10L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员角色不允许修改数据权限");
    }

    @Test
    void getRoleUsers_withUsers_returnsVoList() {
        when(sysUserRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(10L));
        com.campus.wall.entity.user.User user = new com.campus.wall.entity.user.User();
        user.setId(10L);
        user.setUsername("u10");
        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(user));

        List<UserVO> users = roleService.getRoleUsers(2L);

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getUsername()).isEqualTo("u10");
    }

    @Test
    void getRoleUsers_empty_returnsEmpty() {
        when(sysUserRoleMapper.selectUserIdsByRoleId(3L)).thenReturn(List.of());

        assertThat(roleService.getRoleUsers(3L)).isEmpty();
    }

    @Test
    void getAllRoles_mapsDeptIds() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleName("审核员");
        role.setRoleKey("auditor");
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role));
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(2L);
        roleDept.setDeptId(20L);
        when(sysRoleDeptMapper.selectList(any())).thenReturn(List.of(roleDept));

        List<RoleVO> roles = roleService.getAllRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getDeptIds()).containsExactly(20L);
    }

    @Test
    void queryRoles_withKeyword_returnsPageResult() {
        SysRole role = new SysRole();
        role.setId(5L);
        role.setRoleName("运营");
        role.setRoleKey("ops");
        Page<SysRole> page = new Page<>(1, 10);
        page.setRecords(List.of(role));
        page.setTotal(1L);
        when(sysRoleMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<SysRole>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysRole>>any()
        )).thenReturn(page);
        when(sysRoleDeptMapper.selectList(any())).thenReturn(List.of());

        PageResult<RoleVO> result = roleService.queryRoles(1, 10, "运");

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getRoleKey()).isEqualTo("ops");
    }

    @Test
    void getRoleMenuIds_delegatesMapper() {
        when(sysMenuMapper.selectMenuIdsByRoleId(6L)).thenReturn(List.of(1L, 2L));
        assertThat(roleService.getRoleMenuIds(6L)).containsExactly(1L, 2L);
    }

    @Test
    void getRoleDeptIds_nullRole_returnsEmpty() {
        assertThat(roleService.getRoleDeptIds(null)).isEmpty();
    }

    @Test
    void updateRole_missingRole_throws() {
        when(sysRoleMapper.selectById(8L)).thenReturn(null);

        assertThatThrownBy(() -> roleService.updateRole(8L, "n", "k", 0, 1, "r", null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("角色不存在");
    }

    @Test
    void updateRole_changeRoleKey_throws() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleKey("old");
        when(sysRoleMapper.selectById(8L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.updateRole(8L, "n", "new", 0, 1, "r", null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("角色标识不允许修改");
    }

    @Test
    void updateRole_adminNameChange_throws() {
        SysRole role = new SysRole();
        role.setId(9L);
        role.setRoleName("管理员");
        role.setRoleKey(SecurityUtil.getSuperAdminRoleKey());
        role.setStatus(0);
        when(sysRoleMapper.selectById(9L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.updateRole(9L, "新管理员", role.getRoleKey(), 0, 1, "r", null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员角色名称不允许修改");
    }

    @Test
    void updateRole_success_withMenuAssign() {
        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleName("审核");
        role.setRoleKey("reviewer");
        role.setStatus(0);
        when(sysRoleMapper.selectById(10L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));
        when(sysMenuMapper.selectMenuIdsByRoleId(10L)).thenReturn(List.of(1L));
        when(sysUserRoleMapper.selectUserIdsByRoleId(10L)).thenReturn(List.of(2L));
        when(sysRoleDeptMapper.selectDeptIdsByRoleId(10L)).thenReturn(List.of());

        RoleVO vo = roleService.updateRole(10L, "审核2", "reviewer", 1, 8, "备注", List.of(3L));

        assertThat(vo.getRoleName()).isEqualTo("审核2");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(sysRoleMapper).updateById(role);
        verify(sysRoleMenuMapper).deleteByRoleId(10L);
        verify(sysRoleMenuMapper).insert(any(SysRoleMenu.class));
    }

    @Test
    void assignDepts_success_normalizesAndInserts() {
        SysRole role = new SysRole();
        role.setId(12L);
        role.setRoleKey("normal");
        when(sysRoleMapper.selectById(12L)).thenReturn(role);
        when(sysRoleMapper.selectRoleKeysByUserId(99L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));
        when(sysRoleDeptMapper.selectDeptIdsByRoleId(12L)).thenReturn(List.of());

        RoleVO vo = roleService.assignDepts(12L, Arrays.asList(10L, 10L, null, 11L));

        assertThat(vo.getDeptIds()).containsExactly(10L, 11L);
        verify(sysRoleDeptMapper, times(2)).insert(any(SysRoleDept.class));
    }
}
