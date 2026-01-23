package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.RoleDTO;
import com.campus.wall.dto.system.RoleDeleteDTO;
import com.campus.wall.dto.system.RoleDeptDTO;
import com.campus.wall.service.system.RoleService;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.system.RoleVO;
import com.campus.wall.service.system.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@Tag(name = "角色管理", description = "系统角色 CRUD 接口")
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final DeptService deptService;

    @Operation(summary = "角色列表", description = "获取所有角色或分页查询")
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public R<?> list(@RequestParam(value = "page", required = false) Integer page,
                     @RequestParam(value = "size", required = false) Integer size,
                     @RequestParam(value = "keyword", required = false) String keyword) {
        if (page != null && size != null) {
            return R.ok(roleService.queryRoles(page, size, keyword));
        }
        return R.ok(roleService.getAllRoles());
    }

    @Operation(summary = "角色菜单ID列表", description = "获取角色关联的菜单ID列表")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenus(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    @Operation(summary = "创建角色")
    @SaCheckPermission("system:role:add")
    @PostMapping
    public R<RoleVO> create(@RequestBody @Valid RoleDTO dto) {
        RoleVO role = roleService.createRole(dto.getRoleName(), dto.getRoleKey(), dto.getStatus(),
            dto.getSortOrder(), dto.getRemark(), dto.getMenuIds());
        return R.ok(role);
    }

    @Operation(summary = "更新角色")
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}")
    public R<RoleVO> update(@PathVariable Long id, @RequestBody @Valid RoleDTO dto) {
        RoleVO role = roleService.updateRole(id, dto.getRoleName(), dto.getRoleKey(), dto.getStatus(),
            dto.getSortOrder(), dto.getRemark(), dto.getMenuIds());
        return R.ok(role);
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @RequestParam(value = "deleteUsers", required = false) Boolean deleteUsers,
                          @RequestParam(value = "reason", required = false) String reason) {
        roleService.deleteRole(id, Boolean.TRUE.equals(deleteUsers), reason);
        return R.ok();
    }

    @Operation(summary = "删除角色（携带参数）")
    @SaCheckPermission("system:role:delete")
    @PostMapping("/{id}/delete")
    public R<Void> deleteWithBody(@PathVariable Long id, @RequestBody RoleDeleteDTO dto) {
        boolean deleteUsers = dto != null && Boolean.TRUE.equals(dto.getDeleteUsers());
        String reason = dto != null ? dto.getReason() : null;
        roleService.deleteRole(id, deleteUsers, reason);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}/menus")
    public R<RoleVO> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        RoleVO role = roleService.assignMenus(id, menuIds);
        return R.ok(role);
    }

    @Operation(summary = "角色数据权限部门列表", description = "获取角色关联的部门ID列表")
    @SaCheckPermission("system:role:assign")
    @GetMapping("/{id}/depts")
    public R<List<Long>> getRoleDepts(@PathVariable Long id) {
        return R.ok(roleService.getRoleDeptIds(id));
    }

    @Operation(summary = "分配数据权限")
    @SaCheckPermission("system:role:assign")
    @PutMapping("/{id}/depts")
    public R<RoleVO> assignDepts(@PathVariable Long id, @RequestBody RoleDeptDTO dto) {
        RoleVO role = roleService.assignDepts(id, dto.getDeptIds());
        return R.ok(role);
    }

    @Operation(summary = "角色用户列表", description = "获取角色下的用户列表")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/users")
    public R<List<UserVO>> users(@PathVariable Long id) {
        return R.ok(roleService.getRoleUsers(id));
    }

    @Operation(summary = "获取部门树（用于角色授权）", description = "获取部门树结构，用于角色数据权限分配")
    @SaCheckPermission("system:role:assign")
    @GetMapping("/dept-tree")
    public R<List<DeptTreeVO>> getDeptTreeForRole() {
        return R.ok(deptService.getDeptTree());
    }
}
