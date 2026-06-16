package com.campus.wall.service.system;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.campus.wall.common.BusinessException;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.impl.DeptServiceImpl;
import com.campus.wall.service.system.OnlineUserService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.vo.system.DeptTreeVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeptServiceImplTest {

    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private SysRoleDeptMapper roleDeptMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserService userService;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private OperLogService operLogService;

    @InjectMocks
    private DeptServiceImpl deptService;

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
    void create_defaultsAndNormalizeParent() {
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setDeptName(" 研发部 ");

        when(deptMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            SysDept arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        }).when(deptMapper).insert(any(SysDept.class));

        Long id = deptService.create(dept);

        assertThat(id).isEqualTo(100L);
        assertThat(dept.getParentId()).isEqualTo(SecurityConstants.SYSTEM_DEPT_ID);
        assertThat(dept.getDeptName()).isEqualTo("研发部");
        assertThat(dept.getSortOrder()).isEqualTo(0);
        assertThat(dept.getStatus()).isEqualTo(0);
        assertThat(dept.getDataScope()).isEqualTo(SecurityConstants.DATA_SCOPE_DEPT);
    }

    @Test
    void create_invalidDataScope_throws() {
        SysDept dept = new SysDept();
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("测试部");
        dept.setDataScope(99);

        assertThatThrownBy(() -> deptService.create(dept))
            .isInstanceOf(BusinessException.class)
            .hasMessage("数据权限范围不合法");
    }

    @Test
    void create_duplicateSameLevelName_throws() {
        SysDept dept = new SysDept();
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("测试部");
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        when(deptMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> deptService.create(dept))
            .isInstanceOf(BusinessException.class)
            .hasMessage("同级部门名称已存在");
    }

    @Test
    void updateStatus_systemDept_throws() {
        assertThatThrownBy(() -> deptService.updateStatus(SecurityConstants.SYSTEM_DEPT_ID, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("系统部门不允许停用");
    }

    @Test
    void updateStatus_missingDept_throws() {
        when(deptMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> deptService.updateStatus(10L, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("部门不存在");
    }

    @Test
    void updateStatus_disable_kickoutUsersAndLog() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setStatus(0);
        when(deptMapper.selectById(10L)).thenReturn(dept);

        SysDept child = new SysDept();
        child.setId(11L);
        child.setParentId(10L);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept, child));

        User user = new User();
        user.setId(200L);
        user.setDeptId(10L);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(roleMapper.selectRoleKeysByUserId(200L)).thenReturn(List.of());

        deptService.updateStatus(10L, 1);

        assertThat(dept.getStatus()).isEqualTo(1);
        verify(deptMapper).updateById(dept);
        verify(operLogService).log(eq(99L), isNull(), eq("dept"), eq(10L), eq("status_change"), isNull(), any(), any(), isNull());
    }

    @Test
    void delete_systemDept_throws() {
        DeptDeleteDTO dto = new DeptDeleteDTO();
        assertThatThrownBy(() -> deptService.delete(SecurityConstants.SYSTEM_DEPT_ID, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("系统部门不允许删除");
    }

    @Test
    void delete_hasChildDept_throws() {
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        when(deptMapper.selectById(20L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> deptService.delete(20L, new DeptDeleteDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("存在子部门，无法删除，请先删除或转移子部门");
    }

    @Test
    void delete_roleReferenced_throws() {
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        when(deptMapper.selectById(20L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(roleDeptMapper.countRolesByDeptId(20L)).thenReturn(2L);

        assertThatThrownBy(() -> deptService.delete(20L, new DeptDeleteDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该部门已被角色数据权限引用，请先在角色授权中解除关联后再删除");
    }

    @Test
    void delete_deleteStrategyWithoutReason_throws() {
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        when(deptMapper.selectById(20L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(roleDeptMapper.countRolesByDeptId(20L)).thenReturn(0L);

        User user = new User();
        user.setId(201L);
        user.setDeptId(20L);
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        DeptDeleteDTO dto = new DeptDeleteDTO();
        dto.setUserStrategy(DeptDeleteDTO.UserHandleStrategy.DELETE);
        dto.setReason(" ");

        assertThatThrownBy(() -> deptService.delete(20L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("删除用户必须提供原因");
    }

    @Test
    void delete_unassign_movesUsersAndDeletesDept() {
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("测试部");
        dept.setStatus(0);
        when(deptMapper.selectById(20L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(roleDeptMapper.countRolesByDeptId(20L)).thenReturn(0L);

        User u1 = new User();
        u1.setId(301L);
        u1.setDeptId(20L);
        User u2 = new User();
        u2.setId(302L);
        u2.setDeptId(20L);
        when(userMapper.selectList(any())).thenReturn(List.of(u1, u2));
        when(roleMapper.selectRoleKeysByUserId(301L)).thenReturn(List.of());
        when(roleMapper.selectRoleKeysByUserId(302L)).thenReturn(List.of());

        DeptDeleteDTO dto = new DeptDeleteDTO();
        dto.setUserStrategy(DeptDeleteDTO.UserHandleStrategy.UNASSIGN);

        deptService.delete(20L, dto);

        verify(userMapper, times(2)).updateById(any(User.class));
        verify(deptMapper).deleteById(20L);
        assertThat(u1.getDeptId()).isNull();
        assertThat(u2.getDeptId()).isNull();
    }

    @Test
    void delete_transferParent_parentDisabled_throws() {
        SysDept dept = new SysDept();
        dept.setId(21L);
        dept.setParentId(2L);
        when(deptMapper.selectById(21L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(roleDeptMapper.countRolesByDeptId(21L)).thenReturn(0L);

        User user = new User();
        user.setId(401L);
        user.setDeptId(21L);
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        SysDept parent = new SysDept();
        parent.setId(2L);
        parent.setStatus(1);
        when(deptMapper.selectById(2L)).thenReturn(parent);

        DeptDeleteDTO dto = new DeptDeleteDTO();
        dto.setUserStrategy(DeptDeleteDTO.UserHandleStrategy.TRANSFER_PARENT);

        assertThatThrownBy(() -> deptService.delete(21L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("上级部门已停用，无法转移用户，请选择其他处理方式");
    }

    @Test
    void move_selfAsParent_throws() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        when(deptMapper.selectById(10L)).thenReturn(dept);

        assertThatThrownBy(() -> deptService.move(10L, 10L, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能将自己设为上级部门");
    }

    @Test
    void move_toDescendant_throws() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("A");
        when(deptMapper.selectById(10L)).thenReturn(dept);

        SysDept child = new SysDept();
        child.setId(11L);
        child.setParentId(10L);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept, child));

        assertThatThrownBy(() -> deptService.move(10L, 11L, null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能将子孙部门设为上级部门");
    }

    @Test
    void move_sameLevelNameConflict_throws() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("A");
        when(deptMapper.selectById(10L)).thenReturn(dept);

        SysDept parent = new SysDept();
        parent.setId(20L);
        parent.setStatus(0);
        when(deptMapper.selectById(20L)).thenReturn(parent);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept));
        when(deptMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> deptService.move(10L, 20L, null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("同级部门名称已存在");
    }

    @Test
    void move_success_updatesParentAndSort() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setSortOrder(1);
        dept.setDeptName("A");
        when(deptMapper.selectById(10L)).thenReturn(dept);

        SysDept parent = new SysDept();
        parent.setId(20L);
        parent.setStatus(0);
        when(deptMapper.selectById(20L)).thenReturn(parent);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept));
        when(deptMapper.selectCount(any())).thenReturn(0L);

        deptService.move(10L, 20L, 5);

        assertThat(dept.getParentId()).isEqualTo(20L);
        assertThat(dept.getSortOrder()).isEqualTo(5);
        verify(deptMapper).updateById(dept);
        verify(operLogService).log(eq(99L), isNull(), eq("dept"), eq(10L), eq("move"), isNull(), any(), any(), isNull());
    }

    @Test
    void updateSort_null_throws() {
        assertThatThrownBy(() -> deptService.updateSort(10L, null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("排序值不能为空");
    }

    @Test
    void updateSort_success() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setSortOrder(1);
        when(deptMapper.selectById(10L)).thenReturn(dept);

        deptService.updateSort(10L, 9);

        assertThat(dept.getSortOrder()).isEqualTo(9);
        verify(deptMapper).updateById(dept);
    }

    @Test
    void getDeptTree_fallbackToVirtualRootWhenNoTopLevel() {
        SysDept orphan = new SysDept();
        orphan.setId(100L);
        orphan.setParentId(999L);
        orphan.setDeptName("孤儿部门");
        orphan.setSortOrder(1);
        orphan.setStatus(0);
        when(deptMapper.selectList(any())).thenReturn(List.of(orphan));

        List<DeptTreeVO> tree = deptService.getDeptTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getDeptName()).isEqualTo("孤儿部门");
    }

    @Test
    void importDepts_emptyFile_throws() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> deptService.importDepts(file, false))
            .isInstanceOf(BusinessException.class)
            .hasMessage("文件不能为空");
    }

    @Test
    void syncDepts_returnsNotConfiguredMessage() {
        assertThat(deptService.syncDepts()).isEqualTo("未配置同步源，未执行");
    }

    @Test
    void getTree_returnsTopLevelDepts() {
        SysDept root = new SysDept();
        root.setId(1L);
        root.setParentId(0L);
        SysDept child = new SysDept();
        child.setId(2L);
        child.setParentId(1L);
        when(deptMapper.selectList(any())).thenReturn(List.of(root, child));

        List<SysDept> tree = deptService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    void getDeptUsers_andCount_delegateToMapper() {
        User u = new User();
        u.setId(10L);
        u.setUsername("u10");
        when(userMapper.selectList(any())).thenReturn(List.of(u));
        when(userMapper.selectCount(any())).thenReturn(1L);

        var users = deptService.getDeptUsers(8L);
        Long count = deptService.getDeptUserCount(8L);

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getUsername()).isEqualTo("u10");
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void exportDepts_writesExcelResponse() {
        SysDept dept = new SysDept();
        dept.setId(20L);
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDeptName("测试部");
        dept.setStatus(0);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept));

        MockHttpServletResponse response = new MockHttpServletResponse();
        deptService.exportDepts(response);

        assertThat(response.getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void importDepts_ioException_throws() throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> deptService.importDepts(file, false))
            .isInstanceOf(BusinessException.class)
            .hasMessage("读取文件失败");
    }

    @Test
    void importDepts_insertNew_success() throws IOException {
        MockMultipartFile file = buildDeptImportFile(
            List.of("部门名称", "上级部门ID", "排序", "状态", "数据范围", "负责人", "联系电话", "邮箱"),
            List.of("研发部", "1", "2", "正常", "本部门数据权限", "张三", "13800000000", "dev@edu.cn")
        );
        SysDept system = new SysDept();
        system.setId(SecurityConstants.SYSTEM_DEPT_ID);
        system.setDeptName("系统部门");
        when(deptMapper.selectList(isNull())).thenReturn(List.of(system));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doAnswer(invocation -> {
            SysDept arg = invocation.getArgument(0);
            arg.setId(1000L);
            return 1;
        }).when(deptMapper).insert(any(SysDept.class));

        String result = deptService.importDepts(file, false);

        assertThat(result).contains("成功 1 条");
        verify(deptMapper).insert(any(SysDept.class));
    }

    @Test
    void importDepts_updateExisting_updatesDept() throws IOException {
        MockMultipartFile file = buildDeptImportFile(
            List.of("部门名称", "上级部门ID", "排序", "状态", "数据范围"),
            List.of("研发部", "1", "5", "停用", "仅本人数据权限")
        );
        SysDept existing = new SysDept();
        existing.setId(50L);
        existing.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        existing.setDeptName("研发部");
        existing.setStatus(0);
        existing.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        when(deptMapper.selectList(isNull())).thenReturn(List.of(existing));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        String result = deptService.importDepts(file, true);

        assertThat(result).contains("成功 1 条");
        verify(deptMapper).updateById(org.mockito.ArgumentMatchers.argThat(d ->
            d.getId().equals(50L)
                && d.getStatus() == 1
                && d.getDataScope() == SecurityConstants.DATA_SCOPE_SELF
                && d.getSortOrder() == 5
        ));
    }

    @Test
    void importDepts_duplicateWithoutUpdate_marksFailure() throws IOException {
        MockMultipartFile file = buildDeptImportFile(
            List.of("部门名称", "上级部门ID"),
            List.of("研发部", "1")
        );
        SysDept existing = new SysDept();
        existing.setId(50L);
        existing.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        existing.setDeptName("研发部");
        when(deptMapper.selectList(isNull())).thenReturn(List.of(existing));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        String result = deptService.importDepts(file, false);

        assertThat(result).contains("成功 0 条，失败 1 条");
        verify(deptMapper, times(0)).insert(any(SysDept.class));
    }

    @Test
    void delete_transferParent_parentMissing_throws() {
        SysDept dept = new SysDept();
        dept.setId(88L);
        dept.setParentId(777L);
        when(deptMapper.selectById(88L)).thenReturn(dept);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(roleDeptMapper.countRolesByDeptId(88L)).thenReturn(0L);
        User user = new User();
        user.setId(900L);
        user.setDeptId(88L);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(deptMapper.selectById(777L)).thenReturn(null);

        DeptDeleteDTO dto = new DeptDeleteDTO();
        dto.setUserStrategy(DeptDeleteDTO.UserHandleStrategy.TRANSFER_PARENT);

        assertThatThrownBy(() -> deptService.delete(88L, dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("上级部门不存在，无法转移用户");
    }

    private MockMultipartFile buildDeptImportFile(List<Object> headers, List<Object> row) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeHeadRow(headers);
        writer.writeRow(row);
        writer.flush(out);
        writer.close();
        return new MockMultipartFile(
            "file",
            "depts.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            out.toByteArray()
        );
    }
}
