package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-部门关联实体
 */
@Data
@TableName("sys_role_depts")
public class SysRoleDept implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long deptId;
}
