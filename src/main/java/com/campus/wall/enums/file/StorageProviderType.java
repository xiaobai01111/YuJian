package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 存储提供者类型
 */
@Getter
public enum StorageProviderType {
    MINIO("MINIO", "MinIO"),
    LOCAL("LOCAL", "本地"),
    S3("S3", "S3");

    private final String code;
    private final String name;

    StorageProviderType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static StorageProviderType fromCode(String code) {
        if (code == null) {
            return LOCAL;
        }
        for (StorageProviderType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return LOCAL;
    }
}
