package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "EDU邮箱验证请求")
public class VerifyEmailDTO {

    @NotBlank(message = "EDU邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String eduEmail;
}
