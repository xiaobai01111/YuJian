package com.campus.wall.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件记录实体
 */
@Data
@TableName("files")
public class FileRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    private String targetType;

    private String assetType;

    private String publicKey;

    private String filename;

    private String path;

    private Long size;

    private String mimeType;

    private Integer status;

    private Integer auditStatus;

    private String storageClass;

    private String storageProvider;

    private String visibility;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime orphanMarkedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
