package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.entity.system.SysMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.service.system.MenuService;
import com.campus.wall.vo.system.MenuVO;
import com.campus.wall.vo.system.RouterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<MenuVO> getMenuTree() {
        List<SysMenu> allMenus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        );
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public List<MenuVO> getUserMenus(Long userId) {
        // 超级管理员返回所有菜单
        if (StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())) {
            List<SysMenu> allMenus = sysMenuMapper.selectList(
                    new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getType, 0, 1)
                            .eq(SysMenu::getVisible, true)
                            .orderByAsc(SysMenu::getSortOrder)
            );
            return buildMenuTree(allMenus, 0L);
        }
        
        // 普通用户根据角色查询菜单
        List<SysMenu> userMenus = sysMenuMapper.selectMenusByUserId(userId);
        return buildMenuTree(userMenus, 0L);
    }

    @Override
    public List<RouterVO> getUserRoutes() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<SysMenu> menus;
        // 超级管理员返回所有启用的菜单
        if (StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())) {
            menus = sysMenuMapper.selectList(
                    new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getType, 0, 1)
                            .eq(SysMenu::getVisible, true)
                            .eq(SysMenu::getStatus, 0)  // 只返回启用的菜单
                            .orderByAsc(SysMenu::getSortOrder)
            );
        } else {
            // 普通用户根据角色查询菜单（已在mapper中过滤status）
            menus = sysMenuMapper.selectMenusByUserId(userId);
        }
        
        return buildRouterTree(menus, 0L);
    }

    /**
     * 构建路由树
     */
    private List<RouterVO> buildRouterTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .map(menu -> {
                    RouterVO router = new RouterVO();
                    router.setName(menu.getName());
                    router.setPath(menu.getPath());
                    router.setComponent(menu.getComponent());
                    
                    RouterVO.MetaVO meta = new RouterVO.MetaVO();
                    meta.setTitle(menu.getName());
                    meta.setIcon(menu.getIcon());
                    meta.setHidden(!menu.getVisible());
                    meta.setPerms(menu.getPerms());
                    meta.setKeepAlive(false);
                    router.setMeta(meta);
                    
                    List<RouterVO> children = buildRouterTree(menus, menu.getId());
                    if (!children.isEmpty()) {
                        router.setChildren(children);
                    }
                    if (menu.getType() != null && menu.getType() == 0 && children.isEmpty()) {
                        return null;
                    }
                    return router;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Long createMenu(MenuVO menuVO) {
        Objects.requireNonNull(menuVO, "菜单信息不能为空");
        validateMenuPath(menuVO);
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(menuVO, menu);
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void updateMenu(Long menuId, MenuVO menuVO) {
        Objects.requireNonNull(menuVO, "菜单信息不能为空");
        validateMenuPath(menuVO);
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        BeanUtils.copyProperties(menuVO, menu);
        menu.setId(menuId);
        sysMenuMapper.updateById(menu);
    }
    
    /**
     * P2-3: 校验菜单路径
     * - 控制台菜单(顶级菜单)路径必须以/console开头
     * - 子菜单路径不能以/开头（应为相对路径）
     */
    private void validateMenuPath(MenuVO menuVO) {
        String path = menuVO.getPath();
        if (path == null || path.isBlank()) {
            return; // 按钮类型菜单可能没有path
        }
        
        Long parentId = menuVO.getParentId();
        
        // 顶级菜单（parentId为0或null）必须以/console开头
        if (parentId == null || parentId == 0L) {
            if (!path.startsWith("/console")) {
                throw new RuntimeException("控制台顶级菜单路径必须以 /console 开头，当前路径: " + path);
            }
        } else {
            // 子菜单不能以/开头（避免被挂到根级别）
            if (path.startsWith("/")) {
                throw new RuntimeException("子菜单路径不能以 / 开头，请使用相对路径，当前路径: " + path);
            }
        }
        
        // 路径长度限制
        if (path.length() > 200) {
            throw new RuntimeException("菜单路径过长（最大200字符）");
        }
    }

    @Override
    public void deleteMenu(Long menuId) {
        // 检查是否有子菜单
        Long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getParentId, menuId)
        );
        if (childCount > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        sysMenuMapper.deleteById(menuId);
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .map(menu -> {
                    MenuVO vo = new MenuVO();
                    BeanUtils.copyProperties(menu, vo);
                    vo.setChildren(buildMenuTree(menus, menu.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
