package com.campus.wall.util;

import com.campus.wall.constant.FileConstants;

import java.util.UUID;

/**
 * 文件工具类
 */
public final class FileUtil {

    private FileUtil() {}

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 检查是否为允许的图片类型
     */
    public static boolean isAllowedImageType(String mimeType) {
        return FileConstants.ALLOWED_IMAGE_TYPES.contains(mimeType);
    }

    /**
     * 检查是否为允许的图片扩展名
     */
    public static boolean isAllowedImageExtension(String filename) {
        String ext = getExtension(filename);
        return FileConstants.ALLOWED_IMAGE_EXTENSIONS.contains(ext);
    }

    /**
     * 检查文件大小是否超限
     */
    public static boolean isFileSizeExceeded(long size) {
        return size > FileConstants.MAX_FILE_SIZE;
    }

    /**
     * 生成唯一文件名
     */
    public static String generateFilename(String originalFilename) {
        String ext = getExtension(originalFilename);
        return UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
