package com.campus.wall.entity.post;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏实体
 */
@Data
@TableName("bookmarks")
public class Bookmark implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long postId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
