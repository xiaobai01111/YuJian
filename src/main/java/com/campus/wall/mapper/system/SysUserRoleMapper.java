package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_roles WHERE user_id = #{userId}")
    @SuppressWarnings("UnusedReturnValue")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("""
        <script>
        DELETE FROM sys_user_roles
        WHERE user_id IN
        <foreach collection="userIds" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
        </script>
        """)
    @SuppressWarnings("UnusedReturnValue")
    int deleteByUserIds(@Param("userIds") List<Long> userIds);

    @Delete("DELETE FROM sys_user_roles WHERE role_id = #{roleId}")
    @SuppressWarnings("UnusedReturnValue")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT user_id FROM sys_user_roles WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(1) FROM sys_user_roles WHERE role_id = #{roleId}")
    Long countByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT COUNT(1) FROM sys_user_roles WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_roles WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
        <script>
        SELECT user_id, role_id FROM sys_user_roles
        WHERE user_id IN
        <foreach collection="userIds" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
        </script>
        """)
    List<SysUserRole> selectByUserIds(@Param("userIds") List<Long> userIds);

    @Insert("""
        <script>
        INSERT INTO sys_user_roles (user_id, role_id)
        VALUES
        <foreach collection="userRoles" item="item" separator=",">
          (#{item.userId}, #{item.roleId})
        </foreach>
        </script>
        """)
    void batchInsert(@Param("userRoles") List<SysUserRole> userRoles);
}
