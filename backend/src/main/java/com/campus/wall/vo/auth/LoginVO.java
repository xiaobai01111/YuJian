package com.campus.wall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
