package com.campus.wall.controller.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.FileBatchDeleteDTO;
import com.campus.wall.dto.system.FileCleanupRequestDTO;
import com.campus.wall.dto.system.FileQueryDTO;
import com.campus.wall.service.file.FileManageService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.vo.file.FileCategoryVO;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.system.FileCleanupConfigVO;
import com.campus.wall.vo.system.FileCleanupResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件与图库管理
 */
@RestController
@RequestMapping("/api/v1/console")
@RequiredArgsConstructor
public class FileManageController {

    private final FileManageService fileManageService;
    private final FileService fileService;

    @GetMapping("/files")
    public R<PageResult<FileVO>> listFiles(@Validated FileQueryDTO query) {
        return R.ok(fileManageService.queryFiles(query));
    }

    @GetMapping("/files/categories")
    public R<List<FileCategoryVO>> listFileCategories() {
        return R.ok(fileManageService.listFileCategories());
    }

    @PostMapping("/files/upload")
    public R<FileVO> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "type", defaultValue = "file") String targetType,
                                @RequestParam(value = "visibility", required = false) String visibility) {
        return R.ok(fileService.uploadFile(file, targetType, visibility));
    }

    @DeleteMapping("/files/{id}")
    public R<Void> deleteFile(@PathVariable Long id) {
        fileManageService.deleteFile(id);
        return R.ok();
    }

    @PostMapping("/files/batch-delete")
    public R<Void> batchDeleteFiles(@RequestBody @Validated FileBatchDeleteDTO dto) {
        fileManageService.deleteFiles(dto.getIds());
        return R.ok();
    }

    @GetMapping("/gallery")
    public R<PageResult<FileVO>> listGallery(@Validated FileQueryDTO query) {
        return R.ok(fileManageService.queryGallery(query));
    }

    @GetMapping("/gallery/categories")
    public R<List<FileCategoryVO>> listGalleryCategories() {
        return R.ok(fileManageService.listGalleryCategories());
    }

    @PostMapping("/gallery/upload")
    public R<FileVO> uploadGallery(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "type", defaultValue = "gallery") String targetType,
                                   @RequestParam(value = "visibility", required = false) String visibility) {
        return R.ok(fileService.uploadFile(file, targetType, visibility));
    }

    @DeleteMapping("/gallery/{id}")
    public R<Void> deleteGallery(@PathVariable Long id) {
        fileManageService.deleteFile(id);
        return R.ok();
    }

    @PostMapping("/gallery/batch-delete")
    public R<Void> batchDeleteGallery(@RequestBody @Validated FileBatchDeleteDTO dto) {
        fileManageService.deleteFiles(dto.getIds());
        return R.ok();
    }

    @PostMapping("/files/{id}/visibility")
    public R<Void> updateFileVisibility(@PathVariable Long id,
                                        @RequestBody @Validated com.campus.wall.dto.system.FileVisibilityUpdateDTO dto) {
        fileManageService.updateVisibility(id, dto.getVisibility());
        return R.ok();
    }

    @GetMapping("/files/cleanup/config")
    public R<FileCleanupConfigVO> getCleanupConfig() {
        return R.ok(fileService.getCleanupConfig());
    }

    @PutMapping("/files/cleanup/config")
    public R<FileCleanupConfigVO> updateCleanupConfig(@RequestBody @Validated FileCleanupRequestDTO dto) {
        return R.ok(fileService.updateCleanupConfig(dto));
    }

    @PostMapping("/files/cleanup")
    public R<FileCleanupResultVO> cleanOrphanFiles(@RequestBody(required = false) FileCleanupRequestDTO dto) {
        return R.ok(fileService.cleanOrphanFiles(dto));
    }

    @PostMapping("/gallery/{id}/visibility")
    public R<Void> updateGalleryVisibility(@PathVariable Long id,
                                           @RequestBody @Validated com.campus.wall.dto.system.FileVisibilityUpdateDTO dto) {
        fileManageService.updateVisibility(id, dto.getVisibility());
        return R.ok();
    }
}
