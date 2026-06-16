package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户删除DTO（带理由）
 */
@Data
@Schema(description = "用户删除请求")
public class UserDeleteDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    @Schema(description = "用户ID列表")
    private List<Long> ids;

    @Schema(description = "删除理由")
    private String reason;
}
