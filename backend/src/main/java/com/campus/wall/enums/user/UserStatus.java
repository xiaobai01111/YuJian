package com.campus.wall.enums.user;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatus {

    NORMAL(0, "正常"),
    BANNED(1, "封禁");

    private final int code;
    private final String name;

    UserStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
