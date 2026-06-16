package com.campus.wall.controller.system;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.R;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.dto.system.DeptMoveDTO;
import com.campus.wall.dto.system.DeptSortDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.user.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeptControllerTest {

    @Mock
    private DeptService deptService;

    @InjectMocks
    private DeptController deptController;

    @Test
    void list_returnsAllDepts() {
        SysDept dept = dept(10L, 0L, "研发部", null);
        when(deptService.listAll()).thenReturn(List.of(dept));

        R<List<SysDept>> response = deptController.list();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getDeptName()).isEqualTo("研发部");
    }

    @Test
    void tree_buildsChildrenAndDefaultDataScope() {
        SysDept root = dept(SecurityConstants.SYSTEM_DEPT_ID, 0L, "系统", null);
        SysDept child = dept(2L, SecurityConstants.SYSTEM_DEPT_ID, "子部门", null);
        when(deptService.listAll()).thenReturn(List.of(root, child));

        R<List<DeptTreeVO>> response = deptController.tree();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        DeptTreeVO rootVo = response.getData().get(0);
        assertThat(rootVo.getDataScope()).isEqualTo(SecurityConstants.DATA_SCOPE_ALL);
        assertThat(rootVo.getChildren()).hasSize(1);
        assertThat(rootVo.getChildren().get(0).getDataScope()).isEqualTo(SecurityConstants.DATA_SCOPE_DEPT);
    }

    @Test
    void tree_whenNoZeroParent_usesFallbackRoots() {
        SysDept root = dept(10L, 99L, "外部根", null);
        SysDept child = dept(11L, 10L, "外部子", null);
        when(deptService.listAll()).thenReturn(List.of(root, child));

        R<List<DeptTreeVO>> response = deptController.tree();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo(10L);
        assertThat(response.getData().get(0).getChildren()).hasSize(1);
        assertThat(response.getData().get(0).getChildren().get(0).getId()).isEqualTo(11L);
    }

    @Test
    void getById_returnsDept() {
        SysDept dept = dept(7L, 0L, "测试部", SecurityConstants.DATA_SCOPE_DEPT);
        when(deptService.getById(7L)).thenReturn(dept);

        R<SysDept> response = deptController.getById(7L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getDeptName()).isEqualTo("测试部");
    }

    @Test
    void create_update_delete_delegateToService() {
        SysDept dept = dept(null, 0L, "新部门", SecurityConstants.DATA_SCOPE_DEPT);
        when(deptService.create(dept)).thenReturn(100L);

        R<Long> createResp = deptController.create(dept);
        deptController.update(100L, dept);
        deptController.delete(100L);

        assertThat(createResp.getData()).isEqualTo(100L);
        verify(deptService).update(100L, dept);
        verify(deptService).delete(100L, null);
    }

    @Test
    void deleteWithStrategy_delegates() {
        DeptDeleteDTO dto = new DeptDeleteDTO();
        dto.setReason("组织调整");

        deptController.deleteWithStrategy(8L, dto);

        verify(deptService).delete(8L, dto);
    }

    @Test
    void updateStatus_null_throws() {
        SysDept dept = new SysDept();

        assertThatThrownBy(() -> deptController.updateStatus(1L, dept))
            .isInstanceOf(BusinessException.class)
            .hasMessage("状态不能为空");
    }

    @Test
    void updateStatus_invalid_throws() {
        SysDept dept = new SysDept();
        dept.setStatus(2);

        assertThatThrownBy(() -> deptController.updateStatus(1L, dept))
            .isInstanceOf(BusinessException.class)
            .hasMessage("状态只能为0或1");
    }

    @Test
    void updateStatus_valid_delegates() {
        SysDept dept = new SysDept();
        dept.setStatus(1);

        R<Void> response = deptController.updateStatus(5L, dept);

        assertThat(response.getCode()).isEqualTo(200);
        verify(deptService).updateStatus(5L, 1);
    }

    @Test
    void move_sort_export_import_sync_users_delegateToService() {
        DeptMoveDTO moveDTO = new DeptMoveDTO();
        moveDTO.setParentId(3L);
        moveDTO.setSortOrder(9);

        DeptSortDTO sortDTO = new DeptSortDTO();
        sortDTO.setSortOrder(5);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "dept.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[] {1, 2, 3}
        );
        when(deptService.importDepts(file, true)).thenReturn("ok");
        when(deptService.syncDepts()).thenReturn("synced");
        when(deptService.getDeptUsers(2L)).thenReturn(List.of(new UserVO()));
        when(deptService.getDeptUserCount(2L)).thenReturn(6L);

        deptController.move(2L, moveDTO);
        deptController.sort(2L, sortDTO);
        deptController.export(null);
        R<String> importResp = deptController.importDepts(file, true);
        R<String> syncResp = deptController.sync();
        R<List<UserVO>> usersResp = deptController.getDeptUsers(2L);
        R<Long> countResp = deptController.getDeptUserCount(2L);

        verify(deptService).move(2L, 3L, 9);
        verify(deptService).updateSort(2L, 5);
        verify(deptService).exportDepts(null);
        assertThat(importResp.getData()).isEqualTo("ok");
        assertThat(syncResp.getData()).isEqualTo("synced");
        assertThat(usersResp.getData()).hasSize(1);
        assertThat(countResp.getData()).isEqualTo(6L);
    }

    private SysDept dept(Long id, Long parentId, String name, Integer dataScope) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setSortOrder(1);
        dept.setStatus(0);
        dept.setDataScope(dataScope);
        return dept;
    }
}
