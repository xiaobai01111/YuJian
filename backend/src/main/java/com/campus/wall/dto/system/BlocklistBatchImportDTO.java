package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 阻止名单批量导入参数
 */
@Data
public class BlocklistBatchImportDTO {

    @Schema(description = "类型：IP/USER/DEVICE")
    @NotBlank(message = "类型不能为空")
    private String targetType;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "默认原因")
    private String reason;

    @Schema(description = "目标值列表（每行一个）")
    private List<String> values;
}
