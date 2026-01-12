package com.campus.wall.util;

import com.campus.wall.vo.system.MenuVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 树形结构工具类
 */
public final class TreeUtil {

    private TreeUtil() {}

    /**
     * 将菜单列表转换为树形结构
     */
    public static List<MenuVO> buildMenuTree(List<MenuVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        // 按父ID分组
        Map<Long, List<MenuVO>> parentMap = menus.stream()
            .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));

        // 递归构建树
        return buildChildren(parentMap, 0L);
    }

    private static List<MenuVO> buildChildren(Map<Long, List<MenuVO>> parentMap, Long parentId) {
        List<MenuVO> children = parentMap.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }

        for (MenuVO child : children) {
            child.setChildren(buildChildren(parentMap, child.getId()));
        }

        return children;
    }
}
