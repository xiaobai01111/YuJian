package com.campus.wall.controller.file;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.util.HttpHeaderUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileAccessController {

    private final FileRecordMapper fileRecordMapper;
    private final FileAccessService fileAccessService;

    @GetMapping("/preview/{id}")
    public void preview(@PathVariable Long id,
                        @RequestParam(required = false) Long expires,
                        @RequestParam(required = false) String sig,
                        @RequestParam(required = false, defaultValue = "false") boolean download,
                        HttpServletResponse response) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }

        FileVisibility visibility = FileVisibility.fromCode(record.getVisibility());
        if (visibility == FileVisibility.PRIVATE) {
            if (expires == null || !StringUtils.hasText(sig)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "缺少访问签名");
            }
            fileAccessService.verifySignature(id, expires, sig);
            download = false;
        }

        StorageProvider provider = fileAccessService.getProvider(record.getStorageProvider());
        if (provider == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }

        String filename = record.getFilename() != null ? record.getFilename() : ("file-" + id);
        String contentType = record.getMimeType();
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (visibility == FileVisibility.PRIVATE) {
            response.setHeader("Cache-Control", "no-store");
        }

        response.setHeader("Content-Disposition", HttpHeaderUtil.buildContentDisposition(filename, download));

        try (InputStream inputStream = provider.open(record.getPath())) {
            inputStream.transferTo(response.getOutputStream());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }
}
