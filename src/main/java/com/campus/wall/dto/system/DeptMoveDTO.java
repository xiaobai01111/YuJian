package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "部门层级调整请求")
public class DeptMoveDTO {

    @Schema(description = "新的上级部门ID")
    private Long parentId;

    @Schema(description = "排序值（可选）")
    private Integer sortOrder;
}
