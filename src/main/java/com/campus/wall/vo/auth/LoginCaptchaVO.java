package com.campus.wall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录验证码信息")
public class LoginCaptchaVO {

    @Schema(description = "验证码ID")
    private String captchaId;

    @Schema(description = "验证码提示文案")
    private String challenge;

    @Schema(description = "验证码图片（data:image/png;base64,...）")
    private String captchaImage;

    @Schema(description = "过期秒数")
    private Integer expireSeconds;
}
