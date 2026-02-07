package com.campus.wall.entity.post;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 匿名映射实体（树洞帖子真实用户关联）
 */
@Data
@TableName("anonymous_mappings")
public class AnonymousMapping implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private String userIdEncrypted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
