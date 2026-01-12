package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户封禁请求")
public class UserBanDTO {

    @NotNull(message = "封禁状态不能为空")
    @Schema(description = "状态：0正常 1封禁")
    private Integer status;

    @Schema(description = "封禁原因")
    private String reason;
}
