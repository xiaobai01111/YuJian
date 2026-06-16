package com.campus.wall.vo.common;

import lombok.Data;

/**
 * 批量操作结果
 */
@Data
public class BatchActionResultVO {

    private int requested;
    private int success;
    private int skipped;
}
