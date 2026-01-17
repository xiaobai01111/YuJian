package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.vo.system.DeptTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "部门管理", description = "部门管理接口")
@RestController
@RequestMapping("/api/v1/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取部门列表")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/list")
    public R<List<SysDept>> list() {
        return R.ok(deptService.listAll());
    }

    @Operation(summary = "获取部门树")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/tree")
    public R<List<DeptTreeVO>> tree() {
        List<SysDept> allDepts = deptService.listAll();
        List<DeptTreeVO> tree = buildTree(allDepts, 0L);
        return R.ok(tree);
    }

    private List<DeptTreeVO> buildTree(List<SysDept> depts, Long parentId) {
        List<DeptTreeVO> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (dept.getParentId().equals(parentId)) {
                DeptTreeVO vo = new DeptTreeVO();
                vo.setId(dept.getId());
                vo.setParentId(dept.getParentId());
                vo.setDeptName(dept.getDeptName());
                vo.setSortOrder(dept.getSortOrder());
                vo.setLeader(dept.getLeader());
                vo.setPhone(dept.getPhone());
                vo.setEmail(dept.getEmail());
                vo.setStatus(dept.getStatus());
                vo.setCreatedAt(dept.getCreatedAt());
                vo.setChildren(buildTree(depts, dept.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    @Operation(summary = "获取部门详情")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/{id}")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @Operation(summary = "新增部门")
    @SaCheckPermission("system:dept:add")
    @PostMapping
    public R<Long> create(@RequestBody SysDept dept) {
        return R.ok(deptService.create(dept));
    }

    @Operation(summary = "修改部门")
    @SaCheckPermission("system:dept:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        deptService.update(id, dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @SaCheckPermission("system:dept:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
