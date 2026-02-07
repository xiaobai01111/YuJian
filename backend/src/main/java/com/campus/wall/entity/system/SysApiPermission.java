package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API权限配置实体
 * 存储URL与权限标识的映射关系
 */
@Data
@TableName("sys_api_permissions")
public class SysApiPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API路径（支持Ant风格，如 /api/v1/system/user/*）
     */
    private String url;

    /**
     * HTTP方法（GET/POST/PUT/DELETE/*）
     */
    private String httpMethod;

    /**
     * 权限标识（如 system:user:list）
     */
    private String permission;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
