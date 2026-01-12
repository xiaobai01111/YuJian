package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.RoleDTO;
import com.campus.wall.service.system.RoleService;
import com.campus.wall.vo.system.RoleVO;
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
@RequestMapping("/api/v1/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表", description = "获取所有角色")
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public R<List<RoleVO>> list() {
        return R.ok(roleService.getAllRoles());
    }

    @Operation(summary = "创建角色")
    @SaCheckPermission("system:role:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid RoleDTO dto) {
        Long roleId = roleService.createRole(dto.getRoleName(), dto.getRoleKey(), dto.getMenuIds());
        return R.ok(roleId);
    }

    @Operation(summary = "更新角色")
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid RoleDTO dto) {
        roleService.updateRole(id, dto.getRoleName(), dto.getMenuIds());
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @SaCheckPermission("system:role:edit")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
