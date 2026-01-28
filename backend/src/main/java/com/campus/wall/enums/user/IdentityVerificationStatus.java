package com.campus.wall.enums.user;

import lombok.Getter;

/**
 * 身份审核状态枚举
 */
@Getter
public enum IdentityVerificationStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "通过"),
    REJECTED(2, "拒绝"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String name;

    IdentityVerificationStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static IdentityVerificationStatus fromCode(int code) {
        for (IdentityVerificationStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
