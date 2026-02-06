package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户封禁请求")
public class UserBanDTO {

    @Schema(description = "封禁原因")
    private String reason;
}
