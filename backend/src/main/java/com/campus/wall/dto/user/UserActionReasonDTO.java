package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户操作理由 DTO
 */
@Data
@Schema(description = "用户操作理由请求")
public class UserActionReasonDTO {

    @Schema(description = "操作理由")
    private String reason;
}
