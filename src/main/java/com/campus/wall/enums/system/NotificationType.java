package com.campus.wall.enums.system;

import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NotificationType {

    LIKE("like", "点赞通知"),
    COMMENT("comment", "评论通知"),
    REPLY("reply", "回复通知"),
    SYSTEM("system", "系统通知"),
    REPORT("report", "举报处理通知"),
    VERIFICATION("verification", "身份审核通知");

    private final String code;
    private final String name;

    NotificationType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static NotificationType fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
