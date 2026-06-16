package com.campus.wall.service.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.dto.user.UserBatchAssignDTO;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserProfileUpdateDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleWithUserId;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.service.user.impl.UserServiceImpl;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private IdentityVerificationMapper verificationMapper;
    @Mock
    private OperLogService operLogService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(2L);
        lenient().when(dataScopeService.canAccessUser(anyLong(), anyLong())).thenReturn(true);
        lenient().when(dataScopeService.resolveScope(anyLong())).thenReturn(DataScopeService.DataScope.all());
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void assignRoles_superAdminRoleDenied() {
        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleKey(SecurityUtil.getSuperAdminRoleKey());

        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());
        when(sysRoleMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(adminRole));

        assertThatThrownBy(() -> userService.assignRoles(10L, List.of(1L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能分配管理员角色");
    }

    @Test
    void assignRoles_overMenuScopeDenied() {
        SysRole targetRole = new SysRole();
        targetRole.setId(10L);
        targetRole.setRoleKey("user");
        SysRole operatorRole = new SysRole();
        operatorRole.setId(99L);

        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());
        when(sysRoleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(targetRole));
        when(sysRoleMapper.selectRolesByUserId(2L)).thenReturn(List.of(operatorRole));
        when(sysMenuMapper.selectMenuIdsByRoleId(99L)).thenReturn(List.of(1L, 2L));
        when(sysMenuMapper.selectMenuIdsByRoleId(10L)).thenReturn(List.of(1L, 3L));

        assertThatThrownBy(() -> userService.assignRoles(20L, List.of(10L)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能分配超出当前账号权限范围的角色");
    }

    @Test
    void assignRoles_adminBypassStoresRelations() {
        when(sysRoleMapper.selectRoleKeysByUserId(2L))
            .thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        userService.assignRoles(30L, List.of(7L, 8L));

        verify(userRoleMapper).deleteByUserId(30L);
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
        verify(permissionService).clearUserCache(30L);
    }

    @Test
    void updateProfile_emailConflict_throws() {
        User user = new User();
        user.setId(5L);
        when(userMapper.selectById(5L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(1L);

        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNickname("nick");
        dto.setEmail("conflict@edu.cn");

        assertThatThrownBy(() -> userService.updateProfile(5L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("邮箱已被使用");
    }

    @Test
    void updateUser_updateFailed_throws() {
        User user = new User();
        user.setId(6L);
        when(userMapper.selectById(6L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any())).thenReturn(0);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("user@edu.cn");

        assertThatThrownBy(() -> userService.updateUser(6L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("更新用户失败，请重试");
    }

    @Test
    void updateUserStatusWithReason_userNotFound_throws() {
        when(userMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> userService.updateUserStatusWithReason(100L, 1, "x", 2L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateUserStatusWithReason_adminTarget_throws() {
        User target = new User();
        target.setId(10L);
        when(userMapper.selectById(10L)).thenReturn(target);
        when(sysRoleMapper.selectRoleKeysByUserId(10L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        assertThatThrownBy(() -> userService.updateUserStatusWithReason(10L, 1, "违规", 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员账号不允许操作");
    }

    @Test
    void updateUserStatusWithReason_selfOperation_throws() {
        User target = new User();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateUserStatusWithReason(2L, 1, "违规", 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能操作自己的账号");
    }

    @Test
    void updateUserStatusWithReason_banWithoutReason_throws() {
        User target = new User();
        target.setId(9L);
        when(userMapper.selectById(9L)).thenReturn(target);
        when(sysRoleMapper.selectRoleKeysByUserId(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateUserStatusWithReason(9L, 1, "  ", 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("封禁用户必须提供理由");
    }

    @Test
    void updateUserStatusWithReason_updateFail_throws() {
        User target = new User();
        target.setId(9L);
        target.setStatus(0);
        when(userMapper.selectById(9L)).thenReturn(target);
        when(sysRoleMapper.selectRoleKeysByUserId(9L)).thenReturn(List.of());
        when(userMapper.updateStatus(9L, 1)).thenReturn(0);

        assertThatThrownBy(() -> userService.updateUserStatusWithReason(9L, 1, "违规", 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("更新用户状态失败，请重试");
    }

    @Test
    void updateUserStatusWithReason_success_logsAudit() {
        User target = new User();
        target.setId(9L);
        target.setStatus(0);
        when(userMapper.selectById(9L)).thenReturn(target);
        when(sysRoleMapper.selectRoleKeysByUserId(9L)).thenReturn(List.of());
        when(userMapper.updateStatus(9L, 1)).thenReturn(1);

        userService.updateUserStatusWithReason(9L, 1, "违规", 2L);

        verify(operLogService).log(eq(2L), isNull(), eq("user"), eq(9L), eq("ban"), eq("违规"), any(), any(), isNull());
    }

    @Test
    void queryDeletedUsers_incompleteCursor_throws() {
        UserQueryDTO query = new UserQueryDTO();
        query.setLastId(10L);

        assertThatThrownBy(() -> userService.queryDeletedUsers(query))
            .isInstanceOf(BusinessException.class)
            .hasMessage("游标参数不完整");
    }

    @Test
    void queryDeletedUsers_badCursorTime_throws() {
        UserQueryDTO query = new UserQueryDTO();
        query.setLastId(10L);
        query.setLastDeletedAt("invalid");

        assertThatThrownBy(() -> userService.queryDeletedUsers(query))
            .isInstanceOf(BusinessException.class)
            .hasMessage("lastDeletedAt 格式错误");
    }

    @Test
    void queryDeletedUsers_emptyRecords_returnsEmptyPage() {
        UserQueryDTO query = new UserQueryDTO();
        query.setSize(20);
        query.setLoginDateStart("2024-01-01");
        query.setLoginDateEnd("2024-01-31");

        when(userMapper.selectDeletedUsersAfter(
            any(), any(), any(), any(), any(), any(), any(), any(), anyLong(),
            isNull(),
            any(), anyLong()
        ))
            .thenReturn(List.of());
        when(userMapper.countDeletedUsers(
            any(), any(), any(), any(), any(), any(),
            isNull(),
            any(), anyLong()
        )).thenReturn(3L);

        PageResult<?> result = userService.queryDeletedUsers(query);
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void deleteUsersWithReason_empty_noop() {
        userService.deleteUsersWithReason(List.of(), 2L, "x");
        verifyNoInteractions(userMapper, operLogService);
    }

    @Test
    void deleteUsersWithReason_targetAdmin_throws() {
        User user = new User();
        user.setId(10L);
        when(userMapper.selectById(10L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(10L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        assertThatThrownBy(() -> userService.deleteUsersWithReason(List.of(10L), 2L, "x"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能删除管理员账号");
    }

    @Test
    void deleteUsersWithReason_self_throws() {
        User user = new User();
        user.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.deleteUsersWithReason(List.of(2L), 2L, "x"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能删除自己的账号");
    }

    @Test
    void deleteUsersWithReason_success_softDeletesAndLogs() {
        User userA = new User();
        userA.setId(11L);
        userA.setUsername("u11");
        User userB = new User();
        userB.setId(12L);
        userB.setUsername("u12");
        when(userMapper.selectById(11L)).thenReturn(userA);
        when(userMapper.selectById(12L)).thenReturn(userB);
        when(sysRoleMapper.selectRoleKeysByUserId(11L)).thenReturn(List.of());
        when(sysRoleMapper.selectRoleKeysByUserId(12L)).thenReturn(List.of());

        userService.deleteUsersWithReason(List.of(11L, 12L), 2L, "批量清理");

        verify(userMapper).softDeleteById(eq(11L), any(), eq(2L), eq("批量清理"));
        verify(userMapper).softDeleteById(eq(12L), any(), eq(2L), eq("批量清理"));
        verify(operLogService, times(2)).log(eq(2L), isNull(), eq("user"), anyLong(), eq("delete"), eq("批量清理"), any(), isNull(), isNull());
    }

    @Test
    void restoreUser_notDeleted_throws() {
        when(userMapper.selectDeletedById(20L)).thenReturn(null);

        assertThatThrownBy(() -> userService.restoreUser(20L, 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("用户不存在或未被删除");
    }

    @Test
    void restoreUser_updateFailed_throws() {
        User deleted = new User();
        deleted.setId(20L);
        when(userMapper.selectDeletedById(20L)).thenReturn(deleted);
        when(userMapper.restoreById(20L)).thenReturn(0);

        assertThatThrownBy(() -> userService.restoreUser(20L, 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("恢复用户失败，请重试");
    }

    @Test
    void restoreUser_success_logsAudit() {
        User deleted = new User();
        deleted.setId(20L);
        when(userMapper.selectDeletedById(20L)).thenReturn(deleted);
        when(userMapper.restoreById(20L)).thenReturn(1);

        userService.restoreUser(20L, 2L, "误删恢复");

        verify(operLogService).log(eq(2L), isNull(), eq("user"), eq(20L), eq("restore"), eq("误删恢复"), any(), any(), isNull());
    }

    @Test
    void purgeUser_notDeleted_throws() {
        when(userMapper.selectDeletedById(30L)).thenReturn(null);

        assertThatThrownBy(() -> userService.purgeUser(30L, 2L, "x"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("用户不存在或未被删除");
    }

    @Test
    void purgeUser_adminDenied_throws() {
        User deleted = new User();
        deleted.setId(30L);
        deleted.setUsername("admin");
        when(userMapper.selectDeletedById(30L)).thenReturn(deleted);
        when(sysRoleMapper.selectRoleKeysByUserId(30L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        assertThatThrownBy(() -> userService.purgeUser(30L, 2L, "x"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员账号不允许彻底删除");
    }

    @Test
    void purgeUser_deleteFailed_throws() {
        User deleted = new User();
        deleted.setId(31L);
        deleted.setUsername("u31");
        when(userMapper.selectDeletedById(31L)).thenReturn(deleted);
        when(sysRoleMapper.selectRoleKeysByUserId(31L)).thenReturn(List.of());
        when(userMapper.hardDeleteById(31L)).thenReturn(0);

        assertThatThrownBy(() -> userService.purgeUser(31L, 2L, "x"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("彻底删除失败，请重试");
    }

    @Test
    void purgeUser_success_clearsCacheAndLogs() {
        User deleted = new User();
        deleted.setId(31L);
        deleted.setUsername("u31");
        when(userMapper.selectDeletedById(31L)).thenReturn(deleted);
        when(sysRoleMapper.selectRoleKeysByUserId(31L)).thenReturn(List.of());
        when(userMapper.hardDeleteById(31L)).thenReturn(1);

        userService.purgeUser(31L, 2L, "合规删除");

        verify(permissionService).clearUserCache(31L);
        verify(operLogService).log(eq(2L), isNull(), eq("user"), eq(31L), eq("purge"), eq("合规删除"), any(), isNull(), isNull());
    }

    @Test
    void batchAssignByQuery_missingRoles_throws() {
        UserBatchAssignDTO dto = new UserBatchAssignDTO();
        dto.setRoleIds(List.of());

        assertThatThrownBy(() -> userService.batchAssignByQuery(dto, 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("请选择要分配的角色");
    }

    @Test
    void batchAssignByQuery_allNullRoles_throws() {
        UserBatchAssignDTO dto = new UserBatchAssignDTO();
        dto.setRoleIds(java.util.Arrays.asList(null, null));

        assertThatThrownBy(() -> userService.batchAssignByQuery(dto, 2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("请选择要分配的角色");
    }

    @Test
    void editUser_adminCannotChangeDeptOrType() {
        User admin = new User();
        admin.setId(9L);
        when(userMapper.selectById(9L)).thenReturn(admin);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());
        when(sysRoleMapper.selectRoleKeysByUserId(9L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        UserEditDTO dto = new UserEditDTO();
        dto.setDeptId(2L);
        dto.setUserType(1);

        assertThatThrownBy(() -> userService.editUser(9L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员账号不允许调整部门或用户类型");
    }

    @Test
    void updateCreditScore_clampsToRange() {
        User user = new User();
        user.setId(1L);
        user.setCreditScore(95);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateCreditScore(1L, 20, "奖励");

        verify(userMapper).updateCreditScore(1L, 100);
    }

    @Test
    void createUser_duplicateUsername_throws() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("dup");
        dto.setPassword("123");
        dto.setNickname("dup");
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.createUser(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("用户名已存在");
    }

    @Test
    void createUser_withRole_assignsRoleAndLogs() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("u100");
        dto.setPassword("Password@1");
        dto.setNickname("u100");
        dto.setRoleId(9L);

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));
        SysRole role = new SysRole();
        role.setId(9L);
        role.setRoleKey("student");
        when(sysRoleMapper.selectById(9L)).thenReturn(role);
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Long userId = userService.createUser(dto);

        assertThat(userId).isEqualTo(100L);
        verify(userRoleMapper).insert(any(SysUserRole.class));
        verify(operLogService).log("user", 100L, "create", null);
    }

    @Test
    void createUser_nonSuperAdminCannotCreateAdmin() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("u101");
        dto.setPassword("Password@1");
        dto.setNickname("u101");
        dto.setUserType(1);

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.createUser(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("仅超级管理员可创建管理员账号");
    }

    @Test
    void createUser_adminMustBindSuperAdminRole() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("u102");
        dto.setPassword("Password@1");
        dto.setNickname("u102");
        dto.setUserType(1);
        dto.setRoleId(8L);

        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleKey("student");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));
        when(sysRoleMapper.selectById(8L)).thenReturn(role);

        assertThatThrownBy(() -> userService.createUser(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("管理员账号必须绑定超级管理员角色");
    }

    @Test
    void editUser_nonSuperAdminCannotChangeUserType() {
        User user = new User();
        user.setId(12L);
        user.setNickname("old");

        when(userMapper.selectById(12L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());
        when(sysRoleMapper.selectRoleKeysByUserId(12L)).thenReturn(List.of());

        UserEditDTO dto = new UserEditDTO();
        dto.setNickname("new");
        dto.setUserType(0);

        assertThatThrownBy(() -> userService.editUser(12L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("仅超级管理员可修改用户类型");
    }

    @Test
    void editUser_promoteWithoutSuperRoleDenied() {
        User user = new User();
        user.setId(13L);
        user.setNickname("old");

        when(userMapper.selectById(13L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));
        when(sysRoleMapper.selectRoleKeysByUserId(13L)).thenReturn(List.of());

        UserEditDTO dto = new UserEditDTO();
        dto.setNickname("new");
        dto.setUserType(1);

        assertThatThrownBy(() -> userService.editUser(13L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("提升为管理员前请先分配超级管理员角色");
    }

    @Test
    void queryUsers_withCursorAndRoles_mapsRoleLabels() {
        UserQueryDTO query = new UserQueryDTO();
        query.setPage(1);
        query.setSize(10);
        query.setUsername("alice");
        query.setLastId(100L);
        query.setRoleId(3L);
        query.setLoginDateStart("2024-01-01");
        query.setLoginDateEnd("2024-01-31");

        User user = new User();
        user.setId(8L);
        user.setUsername("alice");
        user.setNickname("Alice");
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1L);

        SysRoleWithUserId role = new SysRoleWithUserId();
        role.setUserId(8L);
        role.setRoleName("审核员");
        role.setStatus(1);

        when(userMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<User>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<User>>any()
        )).thenReturn(page);
        when(sysRoleMapper.selectRolesByUserIds(List.of(8L))).thenReturn(List.of(role));

        PageResult<UserVO> result = userService.queryUsers(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getRoles()).contains("审核员(已禁用)");
    }

    @Test
    void getUserDetail_pendingVerification_setsRejectReason() {
        User user = new User();
        user.setId(88L);
        user.setVerifyStatus(0);
        when(userMapper.selectById(88L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(88L)).thenReturn(List.of(1L, 2L));

        IdentityVerification verification = new IdentityVerification();
        verification.setRejectReason("证件不清晰");
        when(verificationMapper.selectOne(any())).thenReturn(verification);

        UserDetailVO detail = userService.getUserDetail(88L);

        assertThat(detail.getRoleIds()).containsExactly(1L, 2L);
        assertThat(detail.getVerifyRejectReason()).isEqualTo("证件不清晰");
    }

    @Test
    void getUserById_withRoles_formatsDisabledRole() {
        User user = new User();
        user.setId(66L);
        user.setUsername("u66");
        when(userMapper.selectById(66L)).thenReturn(user);

        SysRole role = new SysRole();
        role.setRoleName("版主");
        role.setStatus(1);
        when(sysRoleMapper.selectAllRolesByUserId(66L)).thenReturn(List.of(role));

        UserVO vo = userService.getUserById(66L);

        assertThat(vo).isNotNull();
        assertThat(vo.getRoles()).containsExactly("版主(已禁用)");
    }

    @Test
    void updateUserStatus_unban_logsAudit() {
        User user = new User();
        user.setId(77L);
        user.setStatus(1);
        when(userMapper.selectById(77L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(77L)).thenReturn(List.of());
        when(userMapper.updateStatus(77L, 0)).thenReturn(1);

        userService.updateUserStatus(77L, 0);

        verify(operLogService).log(eq(2L), isNull(), eq("user"), eq(77L), eq("unban"), isNull(), any(), any(), isNull());
    }

    @Test
    void batchAssignRoles_assignsToEveryUser() {
        when(sysRoleMapper.selectRoleKeysByUserId(2L))
            .thenReturn(List.of(SecurityUtil.getSuperAdminRoleKey()));

        userService.batchAssignRoles(List.of(41L, 42L), List.of(7L));

        verify(userRoleMapper).deleteByUserId(41L);
        verify(userRoleMapper).deleteByUserId(42L);
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void deleteUsers_delegatesToDeleteUsersWithReason() {
        User user = new User();
        user.setId(50L);
        user.setUsername("u50");
        when(userMapper.selectById(50L)).thenReturn(user);
        when(sysRoleMapper.selectRoleKeysByUserId(50L)).thenReturn(List.of());

        userService.deleteUsers(List.of(50L));

        verify(userMapper).softDeleteById(eq(50L), any(), eq(2L), isNull());
    }

    @Test
    void exportUsers_writesExcelToResponse() {
        User user = new User();
        user.setUsername("u1");
        user.setNickname("昵称1");
        user.setEmail("u1@edu.cn");
        user.setPhone("13800000000");
        user.setUserType(1);
        user.setSex(2);
        user.setStatus(0);
        when(userMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<User>>any()))
            .thenReturn(List.of(user));

        MockHttpServletResponse response = new MockHttpServletResponse();
        userService.exportUsers(new UserQueryDTO(), response);

        assertThat(response.getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void exportUsers_escapesFormulaCells() throws IOException {
        User user = new User();
        user.setUsername("=cmd|'/C calc'!A0");
        user.setNickname("@nick");
        user.setEmail("+user@edu.cn");
        user.setPhone("-13800000000");
        user.setUserType(0);
        user.setSex(0);
        user.setStatus(0);
        when(userMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<User>>any()))
            .thenReturn(List.of(user));

        MockHttpServletResponse response = new MockHttpServletResponse();
        userService.exportUsers(new UserQueryDTO(), response);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("'=cmd|'/C calc'!A0");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("'@nick");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("'+user@edu.cn");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("'-13800000000");
        }
    }

    @Test
    void downloadTemplate_writesTemplateExcel() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        userService.downloadTemplate(response);

        assertThat(response.getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void importUsers_emptyFile_throws() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[] {});

        assertThatThrownBy(() -> userService.importUsers(file, false))
            .isInstanceOf(BusinessException.class)
            .hasMessage("文件不能为空");
    }

    @Test
    void importUsers_success_batchInsert() throws IOException {
        MockMultipartFile file = buildImportFile("u_import_1", "导入用户1", "Pwd@1234", "u1@edu.cn", "13800000001");
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(userMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<User>>any()))
            .thenReturn(List.of());

        String result = userService.importUsers(file, false);

        assertThat(result).contains("成功 1 条");
        verify(userMapper).batchInsert(org.mockito.ArgumentMatchers.<User>anyList());
    }

    @Test
    void importUsers_updateExisting_updatesUser() throws IOException {
        MockMultipartFile file = buildImportFile("u_import_2", "导入用户2", "Pwd@1234", "u2@edu.cn", "13800000002");
        User existing = new User();
        existing.setId(900L);
        existing.setUsername("u_import_2");
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(userMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<User>>any()))
            .thenReturn(List.of(existing));

        String result = userService.importUsers(file, true);

        assertThat(result).contains("成功 1 条");
        verify(userMapper).updateById(existing);
    }

    private MockMultipartFile buildImportFile(String username, String nickname, String password, String email, String phone)
        throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeHeadRow(List.of("用户名", "昵称", "密码", "邮箱", "手机号"));
        writer.writeRow(List.of(username, nickname, password, email, phone));
        writer.flush(out);
        writer.close();
        return new MockMultipartFile(
            "file",
            "users.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            out.toByteArray()
        );
    }
}
