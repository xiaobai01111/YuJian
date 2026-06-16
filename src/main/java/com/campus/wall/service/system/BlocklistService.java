package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.BlocklistBatchImportDTO;
import com.campus.wall.dto.system.BlocklistDTO;
import com.campus.wall.dto.system.BlocklistQueryDTO;
import com.campus.wall.vo.system.BlocklistBatchImportResult;
import com.campus.wall.vo.system.BlocklistVO;

public interface BlocklistService {

    PageResult<BlocklistVO> queryBlocklist(BlocklistQueryDTO query);

    BlocklistVO create(BlocklistDTO dto);

    BlocklistVO update(Long id, BlocklistDTO dto);

    void delete(Long id);

    BlocklistBatchImportResult importBatch(BlocklistBatchImportDTO dto);

    /**
     * 判断目标是否在启用中的阻止名单内（忽略已过期和停用记录）
     */
    boolean isBlocked(String targetType, String targetValue);
}
