package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 文件关联类型枚举
 */
@Getter
public enum FileTargetType {

    POST("post", "帖子"),
    COMMENT("comment", "评论"),
    AVATAR("avatar", "头像"),
    ID_CARD("id_card", "学生证"),
    FILE("file", "文件"),
    GALLERY("gallery", "图库"),
    PUBLIC("public", "公开"),
    PACKAGE("package", "安装包"),
    RESOURCE("resource", "资源");

    private final String code;
    private final String name;

    FileTargetType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileTargetType fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        for (FileTargetType type : values()) {
            if (type.getCode().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
