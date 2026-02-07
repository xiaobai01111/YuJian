package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleWithUserId;
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

    @Select("""
        <script>
        SELECT ur.user_id AS userId, r.role_name AS roleName, r.status AS status
        FROM sys_roles r
        INNER JOIN sys_user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id IN
        <foreach collection="userIds" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
        </script>
        """)
    List<SysRoleWithUserId> selectRolesByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT * FROM sys_roles WHERE role_key = #{roleKey} LIMIT 1")
    SysRole selectByRoleKey(@Param("roleKey") String roleKey);

    @Select("""
        <script>
        SELECT DISTINCT ur.user_id
        FROM sys_user_roles ur
        INNER JOIN sys_roles r ON r.id = ur.role_id
        WHERE r.role_key = #{roleKey}
          <if test="userIds != null and userIds.size() > 0">
            AND ur.user_id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
          </if>
        </script>
        """)
    List<Long> selectUserIdsByRoleKey(@Param("roleKey") String roleKey, @Param("userIds") List<Long> userIds);
}
