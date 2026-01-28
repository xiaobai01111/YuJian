package com.campus.wall.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 身份审核实体
 */
@Data
@TableName("identity_verifications")
public class IdentityVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String imageUrl;

    private String verifyMethod;

    private String studentId;

    private String studentIdHash;

    private Integer status;

    private Long reviewerId;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
