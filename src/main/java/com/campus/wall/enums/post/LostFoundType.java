package com.campus.wall.enums.post;

import lombok.Getter;

/**
 * 失物招领类型枚举
 */
@Getter
public enum LostFoundType {

    LOST("lost", "失物"),
    FOUND("found", "招领");

    private final String code;
    private final String name;

    LostFoundType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static LostFoundType fromCode(String code) {
        for (LostFoundType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
