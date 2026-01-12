package com.campus.wall.mapper.system;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色-部门关联 Mapper
 */
@Mapper
public interface SysRoleDeptMapper {

    @Select("SELECT dept_id FROM sys_role_depts WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_depts WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_depts (role_id, dept_id) VALUES (#{roleId}, #{deptId})")
    void insert(@Param("roleId") Long roleId, @Param("deptId") Long deptId);
}
