package com.campus.wall.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 阻止名单实体
 */
@Data
@TableName("sys_blocklist")
public class SysBlocklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型：IP/USER/DEVICE
     */
    private String targetType;

    /**
     * 目标值
     */
    private String targetValue;

    /**
     * 备注原因
     */
    private String reason;

    /**
     * 状态：0-启用，1-停用
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
