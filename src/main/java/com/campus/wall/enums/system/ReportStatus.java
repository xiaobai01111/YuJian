package com.campus.wall.enums.system;

import lombok.Getter;

/**
 * 举报状态枚举
 */
@Getter
public enum ReportStatus {

    PENDING(0, "待处理"),
    PROCESSED(1, "已处理");

    private final int code;
    private final String name;

    ReportStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ReportStatus fromCode(int code) {
        for (ReportStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
