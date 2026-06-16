package com.campus.wall.controller.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.ApiPermissionDTO;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.service.system.ApiPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "接口权限", description = "动态 URL 权限配置")
@RestController
@RequestMapping("/api/v1/system/api-permissions")
@RequiredArgsConstructor
public class ApiPermissionController {

    private final ApiPermissionService apiPermissionService;

    @Operation(summary = "接口权限列表")
    @GetMapping
    public R<PageResult<SysApiPermission>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Boolean status) {
        return R.ok(apiPermissionService.query(page, size, keyword, status));
    }

    @Operation(summary = "新增接口权限")
    @PostMapping
    public R<SysApiPermission> create(@Valid @RequestBody ApiPermissionDTO dto) {
        return R.ok(apiPermissionService.create(dto));
    }

    @Operation(summary = "更新接口权限")
    @PutMapping("/{id}")
    public R<SysApiPermission> update(@PathVariable Long id, @Valid @RequestBody ApiPermissionDTO dto) {
        return R.ok(apiPermissionService.update(id, dto));
    }

    @Operation(summary = "删除接口权限")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        apiPermissionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "刷新权限缓存")
    @PostMapping("/refresh")
    public R<Void> refresh() {
        apiPermissionService.refreshCache();
        return R.ok();
    }
}
