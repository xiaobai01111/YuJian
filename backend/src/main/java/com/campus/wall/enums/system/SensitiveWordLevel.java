package com.campus.wall.enums.system;

import lombok.Getter;

/**
 * 敏感词级别枚举
 */
@Getter
public enum SensitiveWordLevel {

    WARNING(1, "警告"),
    BLOCK(2, "拦截");

    private final int code;
    private final String name;

    SensitiveWordLevel(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SensitiveWordLevel fromCode(int code) {
        for (SensitiveWordLevel level : values()) {
            if (level.getCode() == code) {
                return level;
            }
        }
        return null;
    }

    public boolean shouldBlock() {
        return this == BLOCK;
    }
}
