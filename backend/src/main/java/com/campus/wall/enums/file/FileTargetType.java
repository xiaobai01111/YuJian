package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 文件关联类型枚举
 */
@Getter
public enum FileTargetType {

    POST("POST", "帖子"),
    COMMENT("COMMENT", "评论"),
    AVATAR("AVATAR", "头像"),
    ID_CARD("ID_CARD", "学生证");

    private final String code;
    private final String name;

    FileTargetType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileTargetType fromCode(String code) {
        for (FileTargetType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
