package com.campus.wall.enums.system;

import lombok.Getter;

/**
 * 举报处理结果枚举
 */
@Getter
public enum ReportResult {

    KEEP("keep", "保留帖子"),
    DELETE_POST("delete_post", "删除帖子"),
    BAN_USER("ban_user", "封禁用户"),
    DELETE_AND_BAN("delete_and_ban", "删除帖子并封禁用户");

    private final String code;
    private final String name;

    ReportResult(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ReportResult fromCode(String code) {
        for (ReportResult result : values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }
}
