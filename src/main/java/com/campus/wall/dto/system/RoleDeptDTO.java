package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色数据权限配置")
public class RoleDeptDTO {

    @Schema(description = "授权部门ID列表")
    private List<Long> deptIds;
}
