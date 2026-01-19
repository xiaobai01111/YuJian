package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阻止名单视图
 */
@Data
public class BlocklistVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "类型：IP/USER/DEVICE")
    private String targetType;

    @Schema(description = "目标值")
    private String targetValue;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "是否已过期")
    private Boolean expired;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建人名称")
    private String createdByName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
