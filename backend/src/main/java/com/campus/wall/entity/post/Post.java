package com.campus.wall.entity.post;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@Data
@TableName("posts")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String board;

    private String title;

    private String content;

    private Boolean isAnonymous;

    private String category;

    private BigDecimal price;

    private String location;

    private LocalDateTime lostTime;

    private Integer status;

    @TableField("show_on_home")
    private Boolean showOnHome;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private LocalDateTime lastInteractionAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
