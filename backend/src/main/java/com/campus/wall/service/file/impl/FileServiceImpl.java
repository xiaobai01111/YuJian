package com.campus.wall.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.config.MinioConfig;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.file.FileService;
import com.campus.wall.vo.file.FileVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileRecordMapper fileRecordMapper;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    // Magic bytes for file type validation
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF

    @Override
    public FileVO uploadFile(MultipartFile file, String targetType) {
        validateFile(file);

        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 确保 bucket 存在
            ensureBucketExists();

            // 生成文件路径
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = generateObjectName(targetType, extension);

            // 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            // 保存文件记录
            FileRecord record = new FileRecord();
            record.setUserId(userId);
            record.setFilename(originalFilename);
            record.setPath(objectName);
            record.setSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setTargetType(targetType);
            record.setStatus(0);
            record.setAuditStatus(0); // 待审核
            record.setStorageClass("STANDARD");
            fileRecordMapper.insert(record);

            return toFileVO(record);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
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
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(record.getPath())
                    .build());
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
                record.setTargetType(targetType);
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
        // 标记超过24小时未绑定的文件
        fileRecordMapper.markOrphanFilesForCleanup();
        
        // 删除超过7天的已标记文件
        List<FileRecord> filesToDelete = fileRecordMapper.selectFilesToDelete();
        for (FileRecord record : filesToDelete) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(record.getPath())
                        .build());
                fileRecordMapper.deleteById(record.getId());
            } catch (Exception e) {
                log.error("清理孤儿文件失败: {}", record.getPath(), e);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // Content-Type 校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        
        // Magic bytes 校验（防止伪造 Content-Type）
        try {
            byte[] header = new byte[12];
            file.getInputStream().read(header);
            if (!isValidImageMagic(header, contentType)) {
                throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED, "文件内容与类型不匹配");
            }
        } catch (java.io.IOException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无法读取文件内容");
        }
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

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
        }
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
        return vo;
    }
}
