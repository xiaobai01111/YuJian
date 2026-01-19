package com.campus.wall.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件分类视图
 */
@Data
public class FileCategoryVO {

    @Schema(description = "分类键")
    private String key;

    @Schema(description = "分类名称")
    private String label;

    @Schema(description = "数量")
    private Long count;
}
