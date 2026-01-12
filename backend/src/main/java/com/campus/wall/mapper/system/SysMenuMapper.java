package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("""
        SELECT DISTINCT m.perms
        FROM sys_menus m
        INNER JOIN sys_role_menus rm ON m.id = rm.menu_id
        INNER JOIN sys_user_roles ur ON rm.role_id = ur.role_id
        INNER JOIN sys_roles r ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.status = 0
          AND m.type = 2
          AND m.perms IS NOT NULL
          AND m.perms != ''
        """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT m.*
        FROM sys_menus m
        INNER JOIN sys_role_menus rm ON m.id = rm.menu_id
        INNER JOIN sys_user_roles ur ON rm.role_id = ur.role_id
        INNER JOIN sys_roles r ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.status = 0
          AND m.type IN (0, 1)
          AND m.visible = true
          AND m.status = 0
        ORDER BY m.sort_order
        """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menus WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT perms FROM sys_menus WHERE perms IS NOT NULL AND perms != ''")
    List<String> selectAllPerms();
}
