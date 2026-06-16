package com.campus.wall.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.wall.config.typehandler.EncryptedStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 身份审核实体
 */
@Data
@TableName(value = "identity_verifications", autoResultMap = true)
public class IdentityVerification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String imageUrl;

    private String verifyMethod;

    @TableField(typeHandler = EncryptedStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String studentId;

    private String studentIdHash;

    private Integer status;

    private Long reviewerId;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
