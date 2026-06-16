package com.campus.wall.vo.system;

import lombok.Data;

@Data
public class FileCleanupResultVO {

    private int marked;
    private int deleted;
    private int failed;
}
