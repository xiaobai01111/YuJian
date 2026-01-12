package com.campus.wall.enums.user;

import lombok.Getter;

/**
 * 用户验证状态枚举
 */
@Getter
public enum VerifyStatus {

    UNVERIFIED(0, "未验证"),
    PRE_VERIFIED(1, "准验证/新生"),
    VERIFIED(2, "已验证");

    private final int code;
    private final String name;

    VerifyStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static VerifyStatus fromCode(int code) {
        for (VerifyStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }

    public boolean canPost() {
        return this == VERIFIED;
    }

    public boolean canPostInFreshman() {
        return this == PRE_VERIFIED || this == VERIFIED;
    }
}
