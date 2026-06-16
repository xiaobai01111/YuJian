package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 在线用户下线参数
 */
@Data
public class OnlineUserKickoutDTO {

    @NotBlank(message = "Token不能为空")
    @Schema(description = "Token")
    private String token;
}
