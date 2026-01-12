package com.campus.wall.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基础 DTO
 */
@Data
@Schema(description = "分页查询基础参数")
public class PageQueryDTO {

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码", defaultValue = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    @Schema(description = "每页大小", defaultValue = "20")
    private Integer size = 20;

    public int getOffset() {
        return (page - 1) * size;
    }
}
