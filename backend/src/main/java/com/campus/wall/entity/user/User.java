package com.campus.wall.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String eduEmail;

    private Integer verifyStatus;

    private String verifyMethod;

    private String studentId;

    private String studentIdHash;

    private Integer status;

    private Integer creditScore;

    private String phone;

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
