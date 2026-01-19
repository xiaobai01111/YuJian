package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阻止名单提交参数
 */
@Data
public class BlocklistDTO {

    @Schema(description = "类型：IP/USER/DEVICE")
    @NotBlank(message = "类型不能为空")
    private String targetType;

    @Schema(description = "目标值")
    @NotBlank(message = "目标值不能为空")
    @Size(max = 128, message = "目标值长度不能超过128")
    private String targetValue;

    @Schema(description = "备注原因")
    @Size(max = 255, message = "备注长度不能超过255")
    private String reason;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;
}
