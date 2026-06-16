package com.campus.wall.controller.file;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileAuditStatus;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.mapper.file.FileRecordMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.util.HttpHeaderUtil;
import com.campus.wall.util.SecurityUtil;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileAccessController {

    private static final String TARGET_TYPE_ID_CARD = "id_card";
    private static final String VERIFICATION_VIEW_PERMISSION = "content:verification:view";
    private static final String VERIFICATION_HANDLE_PERMISSION = "content:verification:handle";

    private final FileRecordMapper fileRecordMapper;
    private final FileAccessService fileAccessService;

    @GetMapping("/preview/{publicKey}")
    public void preview(@PathVariable String publicKey,
                        @RequestParam(required = false) Long expires,
                        @RequestParam(required = false) Long uid,
                        @RequestParam(required = false) String sig,
                        @RequestParam(required = false, defaultValue = "false") boolean download,
                        HttpServletResponse response) {
        if (!StringUtils.hasText(publicKey)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        FileRecord record = fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getPublicKey, publicKey.trim()));
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus() != FileAuditStatus.PASSED.getCode()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "文件审核未通过");
        }

        boolean verificationEvidence = isVerificationEvidence(record);
        FileVisibility visibility = verificationEvidence
            ? FileVisibility.PRIVATE
            : FileVisibility.fromCode(record.getVisibility());
        if (visibility == FileVisibility.PRIVATE) {
            if (expires == null || !StringUtils.hasText(sig)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "缺少访问签名");
            }
            if (verificationEvidence) {
                if (!StpUtil.isLogin()) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "证据材料访问需登录");
                }
                Long currentUserId = StpUtil.getLoginIdAsLong();
                if (!Objects.equals(uid, currentUserId)) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "签名与当前登录态不匹配");
                }
                fileAccessService.verifySignature(publicKey.trim(), expires, sig, uid);
            } else {
                fileAccessService.verifySignature(publicKey.trim(), expires, sig);
            }
            if (!canAccessPrivate(record)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该文件");
            }
            download = false;
        }

        StorageProvider provider = fileAccessService.getProvider(record.getStorageProvider());
        if (provider == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }

        String filename = record.getFilename() != null ? record.getFilename() : ("file-" + record.getId());
        String contentType = record.getMimeType();
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        boolean isImage = contentType.toLowerCase().startsWith("image/");

        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (visibility == FileVisibility.PRIVATE) {
            response.setHeader("Cache-Control", "no-store");
            download = !isImage;
        } else if (!isImage) {
            download = true;
        }

        response.setHeader("Content-Disposition", HttpHeaderUtil.buildContentDisposition(filename, download));

        try (InputStream inputStream = provider.open(record.getPath())) {
            inputStream.transferTo(response.getOutputStream());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    private boolean canAccessPrivate(FileRecord record) {
        if (record == null) {
            return false;
        }
        boolean verificationEvidence = isVerificationEvidence(record);
        if (verificationEvidence && !StpUtil.isLogin()) {
            return false;
        }
        // 私有文件采用签名链接授权；若存在登录态，再附加账号权限约束。
        if (!StpUtil.isLogin()) {
            return true;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        if (record.getUserId() != null && record.getUserId().equals(userId)) {
            return true;
        }
        if (StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())) {
            return true;
        }
        return verificationEvidence && hasVerificationPermission();
    }

    private boolean isVerificationEvidence(FileRecord record) {
        return record != null
            && StringUtils.hasText(record.getTargetType())
            && TARGET_TYPE_ID_CARD.equalsIgnoreCase(record.getTargetType().trim());
    }

    private boolean hasVerificationPermission() {
        return StpUtil.hasPermission(VERIFICATION_VIEW_PERMISSION)
            || StpUtil.hasPermission(VERIFICATION_HANDLE_PERMISSION);
    }
}
