package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
        SELECT r.role_key
        FROM sys_roles r
        INNER JOIN sys_user_roles ur ON r.id = ur.role_id
        INNER JOIN users u ON ur.user_id = u.id
        LEFT JOIN sys_depts d ON u.dept_id = d.id
        WHERE ur.user_id = #{userId}
          AND r.status = 0
          AND u.deleted = 0
          AND (u.dept_id IS NULL OR d.status = 0)
        """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT r.*
        FROM sys_roles r
        INNER JOIN sys_user_roles ur ON r.id = ur.role_id
        INNER JOIN users u ON ur.user_id = u.id
        LEFT JOIN sys_depts d ON u.dept_id = d.id
        WHERE ur.user_id = #{userId}
          AND r.status = 0
          AND u.deleted = 0
          AND (u.dept_id IS NULL OR d.status = 0)
        """)
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT r.*
        FROM sys_roles r
        INNER JOIN sys_user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
        """)
    List<SysRole> selectAllRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM sys_roles WHERE role_key = #{roleKey} LIMIT 1")
    SysRole selectByRoleKey(@Param("roleKey") String roleKey);
}
