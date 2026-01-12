package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.MenuDTO;
import com.campus.wall.service.system.MenuService;
import com.campus.wall.vo.system.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@Tag(name = "菜单管理", description = "系统菜单 CRUD 接口")
@RestController
@RequestMapping("/api/v1/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单列表", description = "获取菜单树状结构")
    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<MenuVO>> list() {
        return R.ok(menuService.getMenuTree());
    }

    @Operation(summary = "创建菜单")
    @SaCheckPermission("system:menu:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid MenuDTO dto) {
        MenuVO menuVO = new MenuVO();
        BeanUtils.copyProperties(dto, menuVO);
        Long menuId = menuService.createMenu(menuVO);
        return R.ok(menuId);
    }

    @Operation(summary = "更新菜单")
    @SaCheckPermission("system:menu:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid MenuDTO dto) {
        MenuVO menuVO = new MenuVO();
        BeanUtils.copyProperties(dto, menuVO);
        menuService.updateMenu(id, menuVO);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission("system:menu:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
