package com.campus.wall.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.FileQueryDTO;
import com.campus.wall.entity.file.FileRecord;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.entity.user.User;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.file.FileAccessService;
import com.campus.wall.service.file.FileManageService;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.vo.file.FileCategoryVO;
import com.campus.wall.vo.file.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件管理服务实现
 */
@Service
@RequiredArgsConstructor
public class FileManageServiceImpl implements FileManageService {

    private static final List<String> FILE_CATEGORY_ORDER = List.of(
        "image", "video", "audio", "document", "archive", "other"
    );

    private static final Map<String, String> FILE_CATEGORY_LABELS = Map.of(
        "image", "图片",
        "video", "视频",
        "audio", "音频",
        "document", "文档",
        "archive", "压缩包",
        "other", "其他"
    );

    private static final List<String> GALLERY_MIME_ORDER = List.of(
        "image/jpeg", "image/png", "image/webp", "image/gif", "image/other"
    );

    private static final Map<String, String> GALLERY_LABELS = Map.of(
        "image/jpeg", "JPEG",
        "image/png", "PNG",
        "image/webp", "WEBP",
        "image/gif", "GIF",
        "image/other", "其他图片"
    );

    private final FileRecordMapper fileRecordMapper;
    private final FileAccessService fileAccessService;
    private final UserMapper userMapper;

    @Override
    public PageResult<FileVO> queryFiles(FileQueryDTO query) {
        return queryByAssetType(query, FileTargetType.FILE.getCode());
    }

    @Override
    public List<FileCategoryVO> listFileCategories() {
        return listFileCategoriesByAssetType(FileTargetType.FILE.getCode());
    }

    @Override
    public PageResult<FileVO> queryResources(FileQueryDTO query) {
        return queryByAssetType(query, FileTargetType.RESOURCE.getCode());
    }

    @Override
    public List<FileCategoryVO> listResourceCategories() {
        return listFileCategoriesByAssetType(FileTargetType.RESOURCE.getCode());
    }

    private PageResult<FileVO> queryByAssetType(FileQueryDTO query, String assetType) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        applyAssetTypeFilter(wrapper, assetType);
        applyKeywordFilter(wrapper, query.getKeyword());
        applyFileCategoryFilter(wrapper, query.getCategory());
        applyVisibilityScope(wrapper);
        wrapper.orderByDesc(FileRecord::getCreatedAt);

        Page<FileRecord> page = fileRecordMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<FileRecord> recordList = page.getRecords();
        Map<Long, User> userMap = loadUserMap(recordList);
        List<FileVO> records = recordList.stream()
            .map(record -> toFileVO(record, userMap))
            .toList();

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public PageResult<FileVO> queryGallery(FileQueryDTO query) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        applyAssetTypeFilter(wrapper, FileTargetType.GALLERY.getCode());
        wrapper.likeRight(FileRecord::getMimeType, "image/");
        applyKeywordFilter(wrapper, query.getKeyword());
        applyGalleryCategoryFilter(wrapper, query.getCategory());
        applyVisibilityScope(wrapper);
        wrapper.orderByDesc(FileRecord::getCreatedAt);

        Page<FileRecord> page = fileRecordMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<FileRecord> recordList = page.getRecords();
        Map<Long, User> userMap = loadUserMap(recordList);
        List<FileVO> records = recordList.stream()
            .map(record -> toFileVO(record, userMap))
            .toList();

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public List<FileCategoryVO> listGalleryCategories() {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
            .select(FileRecord::getMimeType)
            .likeRight(FileRecord::getMimeType, "image/")
            .eq(FileRecord::getAssetType, FileTargetType.GALLERY.getCode());
        applyVisibilityScope(wrapper);
        List<FileRecord> records = fileRecordMapper.selectList(wrapper);

        Map<String, Long> counts = new HashMap<>();
        long total = 0;
        for (FileRecord record : records) {
            total++;
            String key = normalizeGalleryKey(record.getMimeType());
            counts.put(key, counts.getOrDefault(key, 0L) + 1);
        }

        List<FileCategoryVO> result = new ArrayList<>();
        result.add(buildCategory("all", "全部", total));
        for (String key : GALLERY_MIME_ORDER) {
            result.add(buildCategory(key, GALLERY_LABELS.getOrDefault(key, key), counts.getOrDefault(key, 0L)));
        }
        return result;
    }

    @Override
    public void deleteFile(Long fileId) {
        if (fileId == null) {
            return;
        }
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            return;
        }
        deleteRecord(record);
    }

    @Override
    public void deleteFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileRecord> records = fileRecordMapper.selectBatchIds(fileIds);
        for (FileRecord record : records) {
            if (record != null) {
                deleteRecord(record);
            }
        }
    }

    @Override
    public void updateVisibility(Long fileId, String visibility) {
        throw new BusinessException(ResultCode.BAD_REQUEST, "文件可见性由上传策略统一管理");
    }

    private void deleteRecord(FileRecord record) {
        try {
            if (record.getVisibility() != null
                    && FileVisibility.PRIVATE.getCode().equalsIgnoreCase(record.getVisibility())
                    && !canAccessPrivate(record)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该私有文件");
            }
            if (StringUtils.hasText(record.getPath())) {
                StorageProvider provider = fileAccessService.getProvider(record.getStorageProvider());
                if (provider != null) {
                    provider.delete(record.getPath());
                }
            }
            fileRecordMapper.deleteById(record.getId());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    private FileCategoryVO buildCategory(String key, String label, long count) {
        FileCategoryVO vo = new FileCategoryVO();
        vo.setKey(key);
        vo.setLabel(label);
        vo.setCount(count);
        return vo;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<FileRecord> wrapper, String keyword) {
        if (StringUtils.hasText(keyword)) {
            wrapper.like(FileRecord::getFilename, keyword.trim());
        }
    }

    private void applyAssetTypeFilter(LambdaQueryWrapper<FileRecord> wrapper, String assetType) {
        if (StringUtils.hasText(assetType)) {
            wrapper.eq(FileRecord::getAssetType, assetType);
        }
    }

    private List<FileCategoryVO> listFileCategoriesByAssetType(String assetType) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
            .select(FileRecord::getMimeType);
        applyAssetTypeFilter(wrapper, assetType);
        applyVisibilityScope(wrapper);
        List<FileRecord> records = fileRecordMapper.selectList(wrapper);

        Map<String, Long> counts = new HashMap<>();
        long total = 0;
        for (FileRecord record : records) {
            total++;
            String category = resolveFileCategory(record.getMimeType());
            counts.put(category, counts.getOrDefault(category, 0L) + 1);
        }

        List<FileCategoryVO> result = new ArrayList<>();
        result.add(buildCategory("all", "全部", total));
        for (String key : FILE_CATEGORY_ORDER) {
            result.add(buildCategory(key, FILE_CATEGORY_LABELS.getOrDefault(key, key), counts.getOrDefault(key, 0L)));
        }
        return result;
    }

    private void applyFileCategoryFilter(LambdaQueryWrapper<FileRecord> wrapper, String category) {
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category)) {
            return;
        }
        String key = category.trim().toLowerCase();
        switch (key) {
            case "image" -> wrapper.likeRight(FileRecord::getMimeType, "image/");
            case "video" -> wrapper.likeRight(FileRecord::getMimeType, "video/");
            case "audio" -> wrapper.likeRight(FileRecord::getMimeType, "audio/");
            case "document" -> wrapper.and(w -> w.likeRight(FileRecord::getMimeType, "text/")
                .or().like(FileRecord::getMimeType, "application/pdf%")
                .or().like(FileRecord::getMimeType, "application/msword%")
                .or().like(FileRecord::getMimeType, "application/vnd%"));
            case "archive" -> wrapper.and(w -> w.like(FileRecord::getMimeType, "%zip%")
                .or().like(FileRecord::getMimeType, "%rar%")
                .or().like(FileRecord::getMimeType, "%7z%")
                .or().like(FileRecord::getMimeType, "%tar%")
                .or().like(FileRecord::getMimeType, "%gzip%"));
            case "other" -> wrapper.and(w -> w.notLike(FileRecord::getMimeType, "image/%")
                .notLike(FileRecord::getMimeType, "video/%")
                .notLike(FileRecord::getMimeType, "audio/%")
                .notLike(FileRecord::getMimeType, "text/%")
                .notLike(FileRecord::getMimeType, "application/pdf%")
                .notLike(FileRecord::getMimeType, "application/msword%")
                .notLike(FileRecord::getMimeType, "application/vnd%")
                .notLike(FileRecord::getMimeType, "%zip%")
                .notLike(FileRecord::getMimeType, "%rar%")
                .notLike(FileRecord::getMimeType, "%7z%")
                .notLike(FileRecord::getMimeType, "%tar%")
                .notLike(FileRecord::getMimeType, "%gzip%"));
            default -> {
            }
        }
    }

    private void applyVisibilityScope(LambdaQueryWrapper<FileRecord> wrapper) {
        if (wrapper == null) {
            return;
        }
        boolean isAdmin = StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey());
        if (isAdmin) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        wrapper.and(w -> w.eq(FileRecord::getVisibility, FileVisibility.PUBLIC.getCode())
            .or().eq(FileRecord::getUserId, userId));
    }

    private boolean canAccessPrivate(FileRecord record) {
        if (record == null) {
            return false;
        }
        if (StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())) {
            return true;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return record.getUserId() != null && record.getUserId().equals(userId);
    }

    private void applyGalleryCategoryFilter(LambdaQueryWrapper<FileRecord> wrapper, String category) {
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category)) {
            return;
        }
        if ("image/other".equalsIgnoreCase(category)) {
            wrapper.and(w -> w.notLike(FileRecord::getMimeType, "image/jpeg%")
                .notLike(FileRecord::getMimeType, "image/png%")
                .notLike(FileRecord::getMimeType, "image/webp%")
                .notLike(FileRecord::getMimeType, "image/gif%"));
            return;
        }
        wrapper.like(FileRecord::getMimeType, category.trim());
    }

    private String resolveFileCategory(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "other";
        }
        String lower = mimeType.toLowerCase();
        if (lower.startsWith("image/")) return "image";
        if (lower.startsWith("video/")) return "video";
        if (lower.startsWith("audio/")) return "audio";
        if (isArchiveType(lower)) return "archive";
        if (isDocumentType(lower)) return "document";
        return "other";
    }

    private boolean isDocumentType(String mimeType) {
        return mimeType.startsWith("text/")
            || mimeType.startsWith("application/pdf")
            || mimeType.startsWith("application/msword")
            || mimeType.startsWith("application/vnd");
    }

    private boolean isArchiveType(String mimeType) {
        return mimeType.contains("zip")
            || mimeType.contains("rar")
            || mimeType.contains("7z")
            || mimeType.contains("tar")
            || mimeType.contains("gzip");
    }

    private String normalizeGalleryKey(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "image/other";
        }
        String lower = mimeType.toLowerCase();
        if (GALLERY_LABELS.containsKey(lower)) {
            return lower;
        }
        return "image/other";
    }

    private FileVO toFileVO(FileRecord record, Map<Long, User> userMap) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(record, vo);
        vo.setUploaderId(record.getUserId());
        vo.setUploaderName(resolveUploaderName(record.getUserId(), userMap));
        vo.setUrl(fileAccessService.buildAccessUrl(record));
        return vo;
    }

    private Map<Long, User> loadUserMap(List<FileRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = records.stream()
            .map(FileRecord::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> map = new HashMap<>();
        for (User user : users) {
            if (user != null) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private String resolveUploaderName(Long userId, Map<Long, User> userMap) {
        if (userId == null || userMap == null) {
            return null;
        }
        User user = userMap.get(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

}
