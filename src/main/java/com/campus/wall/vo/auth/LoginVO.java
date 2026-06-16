package com.campus.wall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "访问令牌剩余有效期（秒）")
    private Long tokenExpiresIn;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "刷新令牌剩余有效期（秒）")
    private Long refreshTokenExpiresIn;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
