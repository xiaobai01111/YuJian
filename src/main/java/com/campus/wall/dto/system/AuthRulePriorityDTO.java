package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "认证规则优先级更新请求")
public class AuthRulePriorityDTO {

    @NotNull(message = "优先级不能为空")
    @Schema(description = "优先级")
    private Integer priority;
}
