package com.campus.wall.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.wall.config.typehandler.EncryptedStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName(value = "users", autoResultMap = true)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    @TableField(typeHandler = EncryptedStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String email;

    private String emailHash;

    @TableField(typeHandler = EncryptedStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String eduEmail;

    private String eduEmailHash;

    private Integer verifyStatus;

    private String verifyMethod;

    @TableField(typeHandler = EncryptedStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String studentId;

    private String studentIdHash;

    private Integer status;

    private Integer creditScore;

    @TableField(typeHandler = EncryptedStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String phone;

    private String phoneHash;

    private String remark;

    private Long deptId;

    private Integer userType;

    private Integer sex;

    private LocalDateTime loginDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long deletedBy;

    private String deletedReason;

    @TableLogic
    private Integer deleted = 0;
}
