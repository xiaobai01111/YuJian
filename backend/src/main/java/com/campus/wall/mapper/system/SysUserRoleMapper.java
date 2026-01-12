package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_roles WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_roles WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
