package com.campus.wall.dto.system;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阻止名单查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BlocklistQueryDTO extends PageQueryDTO {

    @Schema(description = "类型：IP/USER/DEVICE")
    private String targetType;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "关键词（目标值/原因）")
    private String keyword;
}
