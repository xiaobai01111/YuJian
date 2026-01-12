package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-菜单关联实体
 */
@Data
@TableName("sys_role_menus")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long menuId;
}
