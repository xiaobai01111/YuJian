package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联实体
 */
@Data
@TableName("sys_user_roles")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;
}
