package com.campus.wall.vo.system;

import lombok.Data;

import java.util.List;

/**
 * 阻止名单批量导入结果
 */
@Data
public class BlocklistBatchImportResult {

    private int addedCount;
    private int skippedCount;
    private int invalidCount;
    private List<String> added;
    private List<String> skipped;
    private List<String> invalid;
}
