package com.campus.wall.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户视图")
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private Integer userType;
    private Integer sex;
    private Integer verifyStatus;
    private Integer status;
    private Integer creditScore;
    private Integer deleted;
    private LocalDateTime loginDate;
    private LocalDateTime createdAt;
    private String remark;
    private java.util.List<String> roles;
}
