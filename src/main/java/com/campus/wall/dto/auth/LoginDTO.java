package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过128个字符")
    @Schema(description = "密码")
    private String password;

    @Size(max = 64, message = "验证码ID长度不能超过64个字符")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "验证码ID格式不合法")
    @Schema(description = "登录验证码ID（触发风控时必填）")
    private String captchaId;

    @Size(max = 16, message = "验证码长度不能超过16个字符")
    @Pattern(regexp = "^[A-Za-z0-9+-]*$", message = "验证码格式不合法")
    @Schema(description = "登录验证码答案（触发风控时必填）")
    private String captchaCode;
}
