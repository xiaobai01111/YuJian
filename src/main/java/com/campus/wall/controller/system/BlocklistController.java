package com.campus.wall.controller.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.BlocklistBatchImportDTO;
import com.campus.wall.dto.system.BlocklistDTO;
import com.campus.wall.dto.system.BlocklistQueryDTO;
import com.campus.wall.service.system.BlocklistService;
import com.campus.wall.vo.system.BlocklistBatchImportResult;
import com.campus.wall.vo.system.BlocklistVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 阻止名单管理
 */
@RestController
@RequestMapping("/api/v1/console/monitor/blocklist")
@RequiredArgsConstructor
public class BlocklistController {

    private final BlocklistService blocklistService;

    @GetMapping
    public R<PageResult<BlocklistVO>> list(@Validated BlocklistQueryDTO query) {
        return R.ok(blocklistService.queryBlocklist(query));
    }

    @PostMapping
    public R<BlocklistVO> create(@Valid @RequestBody BlocklistDTO dto) {
        return R.ok(blocklistService.create(dto));
    }

    @PutMapping("/{id}")
    public R<BlocklistVO> update(@PathVariable Long id, @Valid @RequestBody BlocklistDTO dto) {
        return R.ok(blocklistService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        blocklistService.delete(id);
        return R.ok();
    }

    @PostMapping("/batch")
    public R<BlocklistBatchImportResult> importBatch(@Valid @RequestBody BlocklistBatchImportDTO dto) {
        return R.ok(blocklistService.importBatch(dto));
    }
}
