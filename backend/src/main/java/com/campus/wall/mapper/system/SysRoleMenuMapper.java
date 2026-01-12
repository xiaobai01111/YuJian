package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-菜单关联 Mapper
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menus WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menus WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);
}
