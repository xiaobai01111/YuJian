package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "部门排序调整请求")
public class DeptSortDTO {

    @Schema(description = "排序值")
    private Integer sortOrder;
}
