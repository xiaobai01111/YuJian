package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.FileQueryDTO;
import com.campus.wall.service.file.FileManageService;
import com.campus.wall.vo.file.FileCategoryVO;
import com.campus.wall.vo.file.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件与图库管理
 */
@RestController
@RequestMapping("/api/v1/console")
@RequiredArgsConstructor
public class FileManageController {

    private final FileManageService fileManageService;

    @SaCheckPermission("system:file:list")
    @GetMapping("/files")
    public R<PageResult<FileVO>> listFiles(@Validated FileQueryDTO query) {
        return R.ok(fileManageService.queryFiles(query));
    }

    @SaCheckPermission("system:file:list")
    @GetMapping("/files/categories")
    public R<List<FileCategoryVO>> listFileCategories() {
        return R.ok(fileManageService.listFileCategories());
    }

    @SaCheckPermission("system:gallery:list")
    @GetMapping("/gallery")
    public R<PageResult<FileVO>> listGallery(@Validated FileQueryDTO query) {
        return R.ok(fileManageService.queryGallery(query));
    }

    @SaCheckPermission("system:gallery:list")
    @GetMapping("/gallery/categories")
    public R<List<FileCategoryVO>> listGalleryCategories() {
        return R.ok(fileManageService.listGalleryCategories());
    }
}
