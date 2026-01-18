package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    @Select("SELECT dept_id FROM sys_role_depts WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
        SELECT DISTINCT rd.dept_id
        FROM sys_role_depts rd
        INNER JOIN sys_user_roles ur ON rd.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
        """)
    List<Long> selectDeptIdsByUserId(@Param("userId") Long userId);
}
