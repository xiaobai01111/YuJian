package com.campus.wall.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.R;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.service.system.MenuService;
import com.campus.wall.vo.system.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理接口
 */
@Tag(name = "系统管理", description = "动态路由、权限等系统接口")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final MenuService menuService;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 获取当前用户的动态路由（菜单树）
     */
    @Operation(summary = "获取动态路由", description = "根据当前用户角色返回菜单树状结构")
    @GetMapping("/menu/routes")
    public R<List<MenuVO>> getRoutes() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<MenuVO> routes = menuService.getUserMenus(userId);
        return R.ok(routes);
    }

    /**
     * 获取当前用户的权限标识列表
     */
    @Operation(summary = "获取用户权限", description = "返回当前用户的所有权限标识")
    @GetMapping("/user/permissions")
    public R<List<String>> getPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 超级管理员返回通配符权限
        if (userId == 1L) {
            return R.ok(List.of("*"));
        }
        
        List<String> permissions = sysMenuMapper.selectPermsByUserId(userId);
        return R.ok(permissions);
    }
}
