package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色删除请求")
public class RoleDeleteDTO {

    @Schema(description = "是否同时删除该角色下的用户")
    private Boolean deleteUsers = false;

    @Schema(description = "删除原因")
    private String reason;
}
