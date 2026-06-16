package com.campus.wall.dto.system;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OperLogQueryDTO extends PageQueryDTO {

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
