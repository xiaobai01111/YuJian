package com.campus.wall.entity.system;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SysRoleWithUserId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String roleName;
    private Integer status;
}
