package com.campus.wall.enums.post;

import lombok.Getter;

/**
 * 求助分类枚举
 */
@Getter
public enum HelpCategory {

    STUDY("study", "学习"),
    LIFE("life", "生活"),
    OTHER("other", "其他");

    private final String code;
    private final String name;

    HelpCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static HelpCategory fromCode(String code) {
        for (HelpCategory category : values()) {
            if (category.getCode().equals(code)) {
                return category;
            }
        }
        return OTHER;
    }
}
