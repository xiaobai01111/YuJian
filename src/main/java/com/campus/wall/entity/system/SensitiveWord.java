package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感词实体
 */
@Data
@TableName("sensitive_words")
public class SensitiveWord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private Integer level;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
