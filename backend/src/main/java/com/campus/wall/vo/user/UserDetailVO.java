package com.campus.wall.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户详情视图")
public class UserDetailVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String eduEmail;
    private Integer verifyStatus;
    private String verifyMethod;
    private Integer status;
    private Integer creditScore;
    private List<Long> roleIds;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
