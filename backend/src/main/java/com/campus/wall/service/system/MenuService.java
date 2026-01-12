package com.campus.wall.service.system;

import com.campus.wall.vo.system.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 获取菜单树
     */
    List<MenuVO> getMenuTree();

    /**
     * 获取用户菜单（动态路由）
     */
    List<MenuVO> getUserMenus(Long userId);

    /**
     * 创建菜单
     */
    Long createMenu(MenuVO menu);

    /**
     * 更新菜单
     */
    void updateMenu(Long menuId, MenuVO menu);

    /**
     * 删除菜单
     */
    void deleteMenu(Long menuId);
}
