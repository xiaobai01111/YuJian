package com.campus.wall.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.constant.RateLimitConstants;
import com.campus.wall.config.FileCleanupProperties;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.dto.system.FileCleanupRequestDTO;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileAuditStatus;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.content.ContentModerationService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.UploadPolicyService;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.service.storage.StorageProviderRegistry;
import com.campus.wall.util.IpUtil;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.system.FileCleanupConfigVO;
import com.campus.wall.vo.system.FileCleanupResultVO;
import com.campus.wall.entity.system.SysUploadPolicy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文件服务实现 (MinIO S3协议)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;
    private final ContentModerationService contentModerationService;
    private final StorageProviderRegistry storageProviderRegistry;
    private final StorageProperties storageProperties;
    private final FileAccessService fileAccessService;
    private final FileCleanupProperties cleanupProperties;
    private final UploadPolicyService uploadPolicyService;
    private final RateLimitService rateLimitService;
    private final HttpServletRequest request;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ASSET_TYPES = Set.of("file", "gallery", "resource");
    private static final String TARGET_TYPE_ID_CARD = FileTargetType.ID_CARD.getCode();
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024; // 200MB

    // Magic bytes for file type validation
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF

    @Override
    public FileVO uploadFile(MultipartFile file, String targetType, String visibility, String scene) {
        Long userId = StpUtil.getLoginIdAsLong();
        checkUploadRateLimit(userId);

        String normalizedTargetType = normalizeTargetType(targetType);
        validateFile(file, normalizedTargetType);

        String resolvedScene = StringUtils.hasText(scene) ? scene.trim() : normalizedTargetType;
        SysUploadPolicy policy = uploadPolicyService.findByScene(resolvedScene);
        StorageProvider provider = null;
        String storedPath = null;
        boolean inserted = false;
        Long persistedFileId = null;
        try {
            // 生成文件路径
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = generateObjectName(normalizedTargetType, extension);
            String assetType = resolveAssetType(policy, normalizedTargetType, file.getContentType());
            FileVisibility resolvedVisibility = resolveVisibility(normalizedTargetType, visibility, policy);
            provider = resolvePrimaryProvider();
            try {
                storedPath = provider.store(file, objectName);
            } catch (Exception primaryError) {
                StorageProvider fallback = resolveFallbackProvider();
                if (fallback == null || fallback.getType() == provider.getType()) {
                    throw primaryError;
                }
                storedPath = fallback.store(file, objectName);
                provider = fallback;
            }

            // 保存文件记录
            FileRecord record = new FileRecord();
            record.setUserId(userId);
            record.setFilename(originalFilename);
            record.setPath(storedPath);
            record.setSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setTargetType(normalizedTargetType);
            record.setAssetType(assetType);
            record.setPublicKey(UUID.randomUUID().toString().replace("-", ""));
            record.setStatus(0);
            record.setAuditStatus(FileAuditStatus.PENDING.getCode());
            record.setStorageClass("STANDARD");
            record.setStorageProvider(provider.getType().getCode());
            record.setVisibility(resolvedVisibility.getCode());
            fileRecordMapper.insert(record);
            inserted = true;
            persistedFileId = record.getId();

            boolean passed = true;
            if (isAllowedImageType(record.getMimeType())) {
                if (isIdCardTarget(normalizedTargetType)) {
                    // 证据材料不外送第三方图片审核，统一走人工审核链路。
                    passed = true;
                } else {
                    String fileUrl = fileAccessService.buildAccessUrl(record);
                    passed = contentModerationService.moderateImage(fileUrl);
                }
            }
            int auditStatus = passed ? FileAuditStatus.PASSED.getCode() : FileAuditStatus.REJECTED.getCode();
            fileRecordMapper.updateAuditStatus(record.getId(), auditStatus);
            record.setAuditStatus(auditStatus);

            if (!passed) {
                throw new BusinessException(ResultCode.FORBIDDEN, "图片审核未通过");
            }

            return toFileVO(record);

        } catch (Exception e) {
            if (provider != null && StringUtils.hasText(storedPath)) {
                try {
                    provider.delete(storedPath);
                } catch (Exception cleanupError) {
                    log.warn("清理上传文件失败: {}", storedPath, cleanupError);
                }
            }
            if (inserted && persistedFileId != null) {
                try {
                    fileRecordMapper.deleteById(persistedFileId);
                } catch (Exception cleanupError) {
                    log.warn("清理上传记录失败: fileId={}", persistedFileId, cleanupError);
                }
            }
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    private void checkUploadRateLimit(Long userId) {
        if (userId != null) {
            rateLimitService.checkRateLimit(
                "rate:file-upload:user:" + userId,
                RateLimitConstants.FILE_UPLOAD_LIMIT_PER_MINUTE,
                RateLimitConstants.WINDOW_SECONDS,
                ResultCode.TOO_MANY_REQUESTS,
                "上传过于频繁，请稍后再试"
            );
        }
        String clientIp = resolveClientIp();
        rateLimitService.checkRateLimit(
            "rate:file-upload:ip:" + clientIp,
            RateLimitConstants.FILE_UPLOAD_LIMIT_PER_MINUTE_PER_IP,
            RateLimitConstants.WINDOW_SECONDS,
            ResultCode.TOO_MANY_REQUESTS,
            "上传过于频繁，请稍后再试"
        );
    }

    private String resolveClientIp() {
        String ip = request != null ? IpUtil.getClientIp(request) : null;
        return (ip == null || ip.isBlank()) ? "unknown" : ip;
    }

    @Override
    public void deleteFile(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey());
        if (!isAdmin && (record.getUserId() == null || !record.getUserId().equals(userId))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该文件");
        }

        try {
            StorageProvider provider = fileAccessService.getProvider(record.getStorageProvider());
            if (provider != null) {
                provider.delete(record.getPath());
            }
            fileRecordMapper.deleteById(fileId);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void bindFiles(List<Long> fileIds, Long targetId, String targetType) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        String normalizedTargetType = normalizeTargetType(targetType);
        Long userId = StpUtil.getLoginIdAsLong();
        for (Long fileId : fileIds) {
            FileRecord record = fileRecordMapper.selectById(fileId);
            if (record != null) {
                if (record.getUserId() == null || !record.getUserId().equals(userId)) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权绑定该文件");
                }
                if (record.getTargetId() != null && !record.getTargetId().equals(targetId)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "文件已绑定其他资源");
                }
                record.setTargetId(targetId);
                record.setTargetType(normalizedTargetType);
                fileRecordMapper.updateById(record);
            }
        }
    }

    @Override
    public List<FileVO> getFilesByTarget(Long targetId, String targetType) {
        List<FileRecord> records = fileRecordMapper.selectList(
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getTargetId, targetId)
                        .eq(FileRecord::getTargetType, targetType)
        );
        return records.stream().map(this::toFileVO).collect(Collectors.toList());
    }

    @Override
    public void cleanOrphanFiles() {
        cleanOrphanFiles(null);
    }

    @Override
    public FileCleanupConfigVO getCleanupConfig() {
        return buildCleanupConfig(cleanupProperties.getMarkOrphanHours(),
                cleanupProperties.getRetainDays(),
                cleanupProperties.getDeleteLimit());
    }

    @Override
    public FileCleanupConfigVO updateCleanupConfig(FileCleanupRequestDTO dto) {
        if (dto == null) {
            return getCleanupConfig();
        }
        if (dto.getMarkOrphanHours() != null) {
            cleanupProperties.setMarkOrphanHours(dto.getMarkOrphanHours());
        }
        if (dto.getRetainDays() != null) {
            cleanupProperties.setRetainDays(dto.getRetainDays());
        }
        if (dto.getDeleteLimit() != null) {
            cleanupProperties.setDeleteLimit(dto.getDeleteLimit());
        }
        return getCleanupConfig();
    }

    @Override
    public FileCleanupResultVO cleanOrphanFiles(FileCleanupRequestDTO dto) {
        CleanupOptions options = resolveCleanupOptions(dto);

        int marked = fileRecordMapper.markOrphanFilesForCleanupByHours(options.markOrphanHours);
        List<FileRecord> filesToDelete = fileRecordMapper.selectFilesToDeleteByRetention(options.retainDays, options.deleteLimit);

        int deleted = 0;
        int failed = 0;
        for (FileRecord record : filesToDelete) {
            try {
                StorageProvider provider = fileAccessService.getProvider(record.getStorageProvider());
                if (provider != null) {
                    provider.delete(record.getPath());
                }
                fileRecordMapper.deleteById(record.getId());
                deleted++;
            } catch (Exception e) {
                failed++;
                log.error("清理孤儿文件失败: {}", record.getPath(), e);
            }
        }

        FileCleanupResultVO result = new FileCleanupResultVO();
        result.setMarked(marked);
        result.setDeleted(deleted);
        result.setFailed(failed);
        return result;
    }

    private FileCleanupConfigVO buildCleanupConfig(int markOrphanHours, int retainDays, int deleteLimit) {
        FileCleanupConfigVO config = new FileCleanupConfigVO();
        config.setMarkOrphanHours(markOrphanHours);
        config.setRetainDays(retainDays);
        config.setDeleteLimit(deleteLimit);
        return config;
    }

    private CleanupOptions resolveCleanupOptions(FileCleanupRequestDTO dto) {
        int markOrphanHours = cleanupProperties.getMarkOrphanHours();
        int retainDays = cleanupProperties.getRetainDays();
        int deleteLimit = cleanupProperties.getDeleteLimit();
        if (dto != null) {
            if (dto.getMarkOrphanHours() != null) {
                markOrphanHours = dto.getMarkOrphanHours();
            }
            if (dto.getRetainDays() != null) {
                retainDays = dto.getRetainDays();
            }
            if (dto.getDeleteLimit() != null && dto.getDeleteLimit() > 0) {
                deleteLimit = dto.getDeleteLimit();
            }
        }
        if (markOrphanHours < 0) {
            markOrphanHours = 0;
        }
        if (retainDays < 0) {
            retainDays = 0;
        }
        if (deleteLimit <= 0) {
            deleteLimit = 200;
        }
        return new CleanupOptions(markOrphanHours, retainDays, deleteLimit);
    }

    private record CleanupOptions(int markOrphanHours, int retainDays, int deleteLimit) {
    }

    private void validateFile(MultipartFile file, String targetType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        // 文件大小校验
        boolean privileged = isPrivilegedUploader();
        boolean imageOnly = requiresImageOnly(targetType) || !privileged;
        long limit = imageOnly ? MAX_IMAGE_SIZE : MAX_FILE_SIZE;
        if (file.getSize() > limit) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // Content-Type 校验
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (contentType.startsWith("image/")) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
            }
        } else if (imageOnly) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        
        // Magic bytes 校验（防止伪造 Content-Type）
        if (contentType.startsWith("image/")) {
            try (java.io.InputStream inputStream = file.getInputStream()) {
                byte[] header = new byte[12];
                int readBytes = inputStream.read(header);
                if (readBytes < header.length) {
                    throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED, "文件内容与类型不匹配");
                }
                if (!isValidImageMagic(header, contentType)) {
                    throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED, "文件内容与类型不匹配");
                }
            } catch (java.io.IOException e) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "无法读取文件内容");
            }
        }

        if (!contentModerationService.scanFile(file)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "文件安全扫描未通过");
        }
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "type不合法");
        }
        FileTargetType type = FileTargetType.fromCode(targetType);
        if (type == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "type不合法");
        }
        return type.getCode();
    }
    
    private boolean isValidImageMagic(byte[] header, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "image/webp" -> startsWith(header, WEBP_MAGIC) && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
    
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean isAllowedImageType(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    private String generateObjectName(String targetType, String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s/%s.%s", targetType, datePath, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private FileVO toFileVO(FileRecord record) {
        Objects.requireNonNull(record, "文件记录不能为空");
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(record, vo);
        if (FileVisibility.fromCode(record.getVisibility()) == FileVisibility.PRIVATE) {
            vo.setPath(null);
        }
        vo.setUploaderId(record.getUserId());
        vo.setUrl(fileAccessService.buildAccessUrl(record));
        return vo;
    }

    private FileVisibility resolveVisibility(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return FileVisibility.PRIVATE;
        }
        String key = targetType.trim().toLowerCase();
        return switch (key) {
            case "file", "gallery", "public", "package", "resource" -> FileVisibility.PUBLIC;
            default -> FileVisibility.PRIVATE;
        };
    }

    private FileVisibility resolveVisibility(String targetType, String requestedVisibility, SysUploadPolicy policy) {
        if (isIdCardTarget(targetType)) {
            return FileVisibility.PRIVATE;
        }
        if (StringUtils.hasText(requestedVisibility) && canOverrideVisibility()) {
            for (FileVisibility item : FileVisibility.values()) {
                if (item.getCode().equalsIgnoreCase(requestedVisibility.trim())) {
                    return item;
                }
            }
            throw new BusinessException(ResultCode.BAD_REQUEST, "visibility不合法");
        }
        FileVisibility policyVisibility = resolvePolicyVisibility(policy);
        if (policyVisibility != null) {
            return policyVisibility;
        }
        return resolveVisibility(targetType);
    }

    private FileVisibility resolvePolicyVisibility(SysUploadPolicy policy) {
        if (policy == null || !StringUtils.hasText(policy.getVisibility())) {
            return null;
        }
        for (FileVisibility item : FileVisibility.values()) {
            if (item.getCode().equalsIgnoreCase(policy.getVisibility().trim())) {
                return item;
            }
        }
        return null;
    }

    private String resolveAssetType(SysUploadPolicy policy, String targetType, String contentType) {
        if (isIdCardTarget(targetType)) {
            return FileTargetType.RESOURCE.getCode();
        }
        String policyAssetType = normalizeAssetType(policy == null ? null : policy.getAssetType());
        if (policyAssetType != null) {
            return policyAssetType;
        }
        if (ASSET_TYPES.contains(targetType)) {
            return targetType;
        }
        if (contentType != null && contentType.startsWith("image/")) {
            return "gallery";
        }
        return "file";
    }

    private boolean isIdCardTarget(String targetType) {
        return StringUtils.hasText(targetType) && TARGET_TYPE_ID_CARD.equalsIgnoreCase(targetType.trim());
    }

    private String normalizeAssetType(String assetType) {
        if (!StringUtils.hasText(assetType)) {
            return null;
        }
        String normalized = assetType.trim().toLowerCase(Locale.ROOT);
        return ASSET_TYPES.contains(normalized) ? normalized : null;
    }

    private boolean canOverrideVisibility() {
        return StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())
                || StpUtil.hasPermission("system:file:upload")
                || StpUtil.hasPermission("system:gallery:upload")
                || StpUtil.hasPermission("system:resource:upload");
    }

    private boolean isPrivilegedUploader() {
        return StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())
                || StpUtil.hasPermission("system:file:upload")
                || StpUtil.hasPermission("system:gallery:upload")
                || StpUtil.hasPermission("system:resource:upload");
    }

    private boolean requiresImageOnly(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return true;
        }
        String key = targetType.trim().toLowerCase();
        return switch (key) {
            case "file", "public", "package", "resource" -> false;
            default -> true;
        };
    }

    private StorageProvider resolvePrimaryProvider() {
        StorageProviderType type = storageProperties.getPrimaryProvider();
        StorageProvider provider = storageProviderRegistry.getProvider(type);
        if (provider == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "存储提供者不可用");
        }
        return provider;
    }

    private StorageProvider resolveFallbackProvider() {
        StorageProviderType type = storageProperties.getFallbackProvider();
        return storageProviderRegistry.getProvider(type);
    }
}
