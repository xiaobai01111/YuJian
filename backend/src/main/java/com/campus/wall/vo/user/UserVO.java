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
    private Integer verifyStatus;
    private Integer status;
    private Integer creditScore;
    private LocalDateTime createdAt;
}
