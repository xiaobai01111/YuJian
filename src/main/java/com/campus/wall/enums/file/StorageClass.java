package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 存储类型枚举
 */
@Getter
public enum StorageClass {

    STANDARD("STANDARD", "标准存储"),
    IA("IA", "低频访问存储"),
    ARCHIVE("ARCHIVE", "归档存储");

    private final String code;
    private final String name;

    StorageClass(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static StorageClass fromCode(String code) {
        for (StorageClass type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return STANDARD;
    }
}
