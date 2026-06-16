package com.campus.wall.vo.system;

import lombok.Data;

@Data
public class FileCleanupConfigVO {

    private int markOrphanHours;
    private int retainDays;
    private int deleteLimit;
}
