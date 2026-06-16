package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "认证规则状态更新请求")
public class AuthRuleStatusDTO {

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled;
}
