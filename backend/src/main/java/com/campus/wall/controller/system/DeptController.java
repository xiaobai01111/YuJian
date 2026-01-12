package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.R;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.mapper.system.SysDeptMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理接口
 */
@Tag(name = "部门管理", description = "系统部门管理接口")
@RestController
@RequestMapping("/api/v1/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptMapper sysDeptMapper;

    @Operation(summary = "部门列表", description = "获取所有部门（扁平）")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/list")
    public R<List<DeptVO>> list() {
        List<SysDept> depts = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .orderByAsc(SysDept::getParentId)
                        .orderByAsc(SysDept::getSortOrder)
        );
        return R.ok(depts.stream().map(this::toDeptVO).collect(Collectors.toList()));
    }

    @Operation(summary = "部门树", description = "获取部门树形结构")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/tree")
    public R<List<DeptVO>> tree() {
        List<SysDept> depts = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .orderByAsc(SysDept::getSortOrder)
        );
        List<DeptVO> deptVOs = depts.stream().map(this::toDeptVO).collect(Collectors.toList());
        return R.ok(buildTree(deptVOs));
    }

    @Operation(summary = "新增部门")
    @SaCheckPermission("system:dept:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid DeptDTO dto) {
        // 校验父部门是否存在
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            SysDept parent = sysDeptMapper.selectById(dto.getParentId());
            if (parent == null) {
                return R.fail("父部门不存在");
            }
        }
        
        SysDept dept = new SysDept();
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        dept.setDeptName(dto.getDeptName());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        
        sysDeptMapper.insert(dept);
        return R.ok(dept.getId());
    }

    @Operation(summary = "修改部门")
    @SaCheckPermission("system:dept:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid DeptDTO dto) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            return R.fail("部门不存在");
        }
        
        // 不能将部门设为自己的子部门
        if (dto.getParentId() != null && dto.getParentId().equals(id)) {
            return R.fail("不能将部门设为自己的子部门");
        }
        
        // 检查是否会形成循环
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            if (isDescendant(id, dto.getParentId())) {
                return R.fail("不能将部门移动到其子部门下");
            }
        }
        
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        dept.setDeptName(dto.getDeptName());
        dept.setSortOrder(dto.getSortOrder());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus());
        dept.setUpdatedAt(LocalDateTime.now());
        
        sysDeptMapper.updateById(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @SaCheckPermission("system:dept:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        // 检查是否有子部门
        Long childCount = sysDeptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)
        );
        if (childCount > 0) {
            return R.fail("存在子部门，无法删除");
        }
        
        sysDeptMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "获取部门详情")
    @SaCheckPermission("system:dept:list")
    @GetMapping("/{id}")
    public R<DeptVO> getById(@PathVariable Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            return R.fail("部门不存在");
        }
        return R.ok(toDeptVO(dept));
    }

    private boolean isDescendant(Long ancestorId, Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        while (dept != null && dept.getParentId() != null && dept.getParentId() > 0) {
            if (dept.getParentId().equals(ancestorId)) {
                return true;
            }
            dept = sysDeptMapper.selectById(dept.getParentId());
        }
        return false;
    }

    private List<DeptVO> buildTree(List<DeptVO> depts) {
        Map<Long, DeptVO> deptMap = depts.stream()
                .collect(Collectors.toMap(DeptVO::getId, d -> d));
        
        List<DeptVO> rootDepts = new ArrayList<>();
        
        for (DeptVO dept : depts) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                rootDepts.add(dept);
            } else {
                DeptVO parent = deptMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        
        return rootDepts;
    }

    private DeptVO toDeptVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setSortOrder(dept.getSortOrder());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setStatus(dept.getStatus());
        vo.setCreatedAt(dept.getCreatedAt());
        return vo;
    }

    @Data
    public static class DeptDTO {
        private Long parentId;
        @NotBlank(message = "部门名称不能为空")
        private String deptName;
        private Integer sortOrder;
        private String leader;
        private String phone;
        private String email;
        private Integer status;
    }

    @Data
    public static class DeptVO {
        private Long id;
        private Long parentId;
        private String deptName;
        private Integer sortOrder;
        private String leader;
        private String phone;
        private String email;
        private Integer status;
        private LocalDateTime createdAt;
        private List<DeptVO> children;
    }
}
