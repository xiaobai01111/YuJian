package com.campus.wall.constant;

import java.util.Set;

/**
 * 文件相关常量
 */
public final class FileConstants {

    private FileConstants() {}

    /**
     * 允许的图片格式
     */
    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );

    /**
     * 允许的图片扩展名
     */
    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "webp"
    );

    /**
     * 最大文件大小（5MB）
     */
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传目录 - 帖子图片
     */
    public static final String UPLOAD_DIR_POST = "posts/";

    /**
     * 上传目录 - 头像
     */
    public static final String UPLOAD_DIR_AVATAR = "avatars/";

    /**
     * 上传目录 - 身份证
     */
    public static final String UPLOAD_DIR_ID_CARD = "id-cards/";
}
