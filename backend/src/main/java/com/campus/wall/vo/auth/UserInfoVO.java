package com.campus.wall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户信息")
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private Integer verifyStatus;
    private String verifyMethod;
    private Integer creditScore;
    private List<String> roles;
    private List<String> permissions;
    private LocalDateTime createdAt;
}
