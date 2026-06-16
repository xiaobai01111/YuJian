package com.campus.wall.dto.system;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FileCleanupRequestDTO {

    /**
     * 未绑定超过多少小时后标记为待清理
     */
    @Min(0)
    private Integer markOrphanHours;

    /**
     * 标记后保留天数
     */
    @Min(0)
    private Integer retainDays;

    /**
     * 单次删除上限
     */
    @Min(1)
    private Integer deleteLimit;
}
