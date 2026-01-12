package com.campus.wall.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.R;
import com.campus.wall.service.file.FileService;
import com.campus.wall.vo.file.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 */
@Tag(name = "文件管理", description = "文件上传下载接口")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件", description = "上传图片文件，支持 JPG/PNG/WEBP/GIF，最大 5MB")
    @PostMapping("/upload")
    public R<FileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "post") String targetType) {
        StpUtil.checkLogin();
        FileVO fileVO = fileService.uploadFile(file, targetType);
        return R.ok(fileVO);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        StpUtil.checkLogin();
        fileService.deleteFile(id);
        return R.ok();
    }
}
