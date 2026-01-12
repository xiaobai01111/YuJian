package com.campus.wall.enums.user;

import lombok.Getter;

/**
 * 身份验证方式枚举
 */
@Getter
public enum VerifyMethod {

    EDU_EMAIL("EDU_EMAIL", "EDU邮箱验证"),
    ID_CARD_OCR("ID_CARD_OCR", "学生证OCR识别"),
    MANUAL("MANUAL", "人工审核");

    private final String code;
    private final String name;

    VerifyMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static VerifyMethod fromCode(String code) {
        for (VerifyMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}
