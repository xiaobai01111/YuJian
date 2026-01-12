package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 文件审核状态枚举
 */
@Getter
public enum FileAuditStatus {

    PENDING(0, "待审核"),
    PASSED(1, "审核通过"),
    REJECTED(2, "违规");

    private final int code;
    private final String name;

    FileAuditStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileAuditStatus fromCode(int code) {
        for (FileAuditStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
