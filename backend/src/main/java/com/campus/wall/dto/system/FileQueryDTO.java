package com.campus.wall.dto.system;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件管理查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class FileQueryDTO extends PageQueryDTO {

    @Schema(description = "分类")
    private String category;

    @Schema(description = "关键词（文件名）")
    private String keyword;
}
