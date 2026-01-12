package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.service.system.MenuService;
import com.campus.wall.vo.system.MenuVO;
import com.campus.wall.vo.system.RouterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单接口
 * 提供路由和菜单树获取功能（用于角色权限分配）
 */
@Tag(name = "菜单", description = "系统菜单接口")
@RestController
@RequestMapping("/api/v1/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取当前用户路由", description = "根据用户权限返回可访问的路由")
    @SaCheckLogin
    @GetMapping("/routes")
    public R<List<RouterVO>> getRoutes() {
        return R.ok(menuService.getUserRoutes());
    }

    @Operation(summary = "菜单列表", description = "获取菜单树状结构（用于角色权限分配）")
    @SaCheckPermission("system:role:assign")
    @GetMapping("/list")
    public R<List<MenuVO>> list() {
        return R.ok(menuService.getMenuTree());
    }
}
