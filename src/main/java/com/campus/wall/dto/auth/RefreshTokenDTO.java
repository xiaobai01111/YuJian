package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenDTO {

    @Schema(description = "刷新令牌（可选；未提供时从HttpOnly Cookie读取）")
    @Size(min = 16, max = 128, message = "刷新令牌长度不合法")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "刷新令牌格式不合法")
    private String refreshToken;
}
