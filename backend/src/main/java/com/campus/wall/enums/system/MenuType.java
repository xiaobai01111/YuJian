package com.campus.wall.enums.system;

import lombok.Getter;

/**
 * 菜单类型枚举
 */
@Getter
public enum MenuType {

    DIRECTORY(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int code;
    private final String name;

    MenuType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MenuType fromCode(int code) {
        for (MenuType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }

    public boolean isRoute() {
        return this == DIRECTORY || this == MENU;
    }
}
