package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 文件状态枚举
 */
@Getter
public enum FileStatus {

    NORMAL(0, "正常"),
    PENDING_CLEANUP(1, "待清理"),
    DELETED(2, "已删除");

    private final int code;
    private final String name;

    FileStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileStatus fromCode(int code) {
        for (FileStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
