package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统菜单实体
 */
@Data
@TableName("sys_menus")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private String component;

    private String perms;

    private String icon;

    /**
     * 菜单分组编码：WORKBENCH/IAM/CONTENT/MONITOR/TOOLS/CAMPUS/GENERAL
     */
    private String groupCode;

    private Integer type;

    private Boolean visible;

    private Integer status;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
