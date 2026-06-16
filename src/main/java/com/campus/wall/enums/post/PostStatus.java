package com.campus.wall.enums.post;

import lombok.Getter;

/**
 * 帖子状态枚举
 */
@Getter
public enum PostStatus {

    NORMAL(0, "正常"),
    RESOLVED(1, "已解决"),
    DELETED(2, "已删除"),
    PENDING_AUDIT(3, "待审核"),
    ARCHIVED(4, "已下架"),
    SOLD(5, "已售出");

    private final int code;
    private final String name;

    PostStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PostStatus fromCode(int code) {
        for (PostStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 是否在前端列表中可见（只有删除、待审核、下架不可见）
     */
    public boolean isVisible() {
        return this == NORMAL || this == RESOLVED || this == SOLD;
    }
}
