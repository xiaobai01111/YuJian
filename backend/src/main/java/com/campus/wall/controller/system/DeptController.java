package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.dto.system.DeptMoveDTO;
import com.campus.wall.dto.system.DeptSortDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "部门管理", description = "部门管理接口")
@RestController
@RequestMapping("/api/v1/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    public R<List<SysDept>> list() {
        return R.ok(deptService.listAll());
    }

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    public R<List<DeptTreeVO>> tree() {
        List<SysDept> allDepts = deptService.listAll();
        List<DeptTreeVO> tree = buildTree(allDepts, 0L);
        if (tree.isEmpty() && !allDepts.isEmpty()) {
            tree = buildRootTree(allDepts);
        }
        return R.ok(tree);
    }

    private List<DeptTreeVO> buildTree(List<SysDept> depts, Long parentId) {
        List<DeptTreeVO> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (java.util.Objects.equals(dept.getParentId(), parentId)) {
                DeptTreeVO vo = new DeptTreeVO();
                vo.setId(dept.getId());
                vo.setParentId(dept.getParentId());
                vo.setDeptName(dept.getDeptName());
                vo.setSortOrder(dept.getSortOrder());
                vo.setLeader(dept.getLeader());
                vo.setPhone(dept.getPhone());
                vo.setEmail(dept.getEmail());
                vo.setStatus(dept.getStatus());
                Integer dataScope = dept.getDataScope();
                if (dataScope == null) {
                    dataScope = dept.getId() != null && dept.getId().equals(SecurityConstants.SYSTEM_DEPT_ID)
                        ? SecurityConstants.DATA_SCOPE_ALL
                        : SecurityConstants.DATA_SCOPE_DEPT;
                }
                vo.setDataScope(dataScope);
                vo.setCreatedAt(dept.getCreatedAt());
                vo.setChildren(buildTree(depts, dept.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private List<DeptTreeVO> buildRootTree(List<SysDept> depts) {
        List<DeptTreeVO> roots = new ArrayList<>();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (SysDept dept : depts) {
            if (dept.getId() != null) {
                ids.add(dept.getId());
            }
        }
        for (SysDept dept : depts) {
            Long parentId = dept.getParentId();
            boolean isRoot = parentId == null
                || parentId == 0L
                || !ids.contains(parentId)
                || parentId.equals(dept.getId());
            if (isRoot) {
                DeptTreeVO vo = new DeptTreeVO();
                vo.setId(dept.getId());
                vo.setParentId(dept.getParentId());
                vo.setDeptName(dept.getDeptName());
                vo.setSortOrder(dept.getSortOrder());
                vo.setLeader(dept.getLeader());
                vo.setPhone(dept.getPhone());
                vo.setEmail(dept.getEmail());
                vo.setStatus(dept.getStatus());
                Integer dataScope = dept.getDataScope();
                if (dataScope == null) {
                    dataScope = dept.getId() != null && dept.getId().equals(SecurityConstants.SYSTEM_DEPT_ID)
                        ? SecurityConstants.DATA_SCOPE_ALL
                        : SecurityConstants.DATA_SCOPE_DEPT;
                }
                vo.setDataScope(dataScope);
                vo.setCreatedAt(dept.getCreatedAt());
                vo.setChildren(buildTree(depts, dept.getId()));
                roots.add(vo);
            }
        }
        return roots;
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public R<Long> create(@RequestBody SysDept dept) {
        return R.ok(deptService.create(dept));
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        deptService.update(id, dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id, null);
        return R.ok();
    }

    @Operation(summary = "删除部门（带用户处理策略）")
    @PostMapping("/{id}/delete")
    public R<Void> deleteWithStrategy(@PathVariable Long id, @RequestBody DeptDeleteDTO dto) {
        deptService.delete(id, dto);
        return R.ok();
    }

    @Operation(summary = "更新部门状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody SysDept dept) {
        if (dept.getStatus() == null) {
            throw new com.campus.wall.common.BusinessException("状态不能为空");
        }
        if (dept.getStatus() != 0 && dept.getStatus() != 1) {
            throw new com.campus.wall.common.BusinessException("状态只能为0或1");
        }
        deptService.updateStatus(id, dept.getStatus());
        return R.ok();
    }

    @Operation(summary = "调整部门层级")
    @PutMapping("/{id}/move")
    public R<Void> move(@PathVariable Long id, @RequestBody DeptMoveDTO dto) {
        deptService.move(id, dto.getParentId(), dto.getSortOrder());
        return R.ok();
    }

    @Operation(summary = "调整部门排序")
    @PutMapping("/{id}/sort")
    public R<Void> sort(@PathVariable Long id, @RequestBody DeptSortDTO dto) {
        deptService.updateSort(id, dto.getSortOrder());
        return R.ok();
    }

    @Operation(summary = "导出部门列表", description = "导出部门列表到Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        deptService.exportDepts(response);
    }

    @Operation(summary = "导入部门", description = "从Excel导入部门")
    @PostMapping("/import")
    public R<String> importDepts(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "updateExisting", defaultValue = "false") boolean updateExisting) {
        return R.ok(deptService.importDepts(file, updateExisting));
    }

    @Operation(summary = "同步部门", description = "从外部系统同步部门")
    @PostMapping("/sync")
    public R<String> sync() {
        return R.ok(deptService.syncDepts());
    }

    @Operation(summary = "获取部门用户列表")
    @GetMapping("/{id}/users")
    public R<List<UserVO>> getDeptUsers(@PathVariable Long id) {
        return R.ok(deptService.getDeptUsers(id));
    }

    @Operation(summary = "获取部门用户数量")
    @GetMapping("/{id}/user-count")
    public R<Long> getDeptUserCount(@PathVariable Long id) {
        return R.ok(deptService.getDeptUserCount(id));
    }
}
