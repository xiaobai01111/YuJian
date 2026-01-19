package com.campus.wall.enums.file;

import lombok.Getter;

/**
 * 文件可见性
 */
@Getter
public enum FileVisibility {
    PRIVATE("PRIVATE", "私有"),
    PUBLIC("PUBLIC", "公有");

    private final String code;
    private final String name;

    FileVisibility(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileVisibility fromCode(String code) {
        if (code == null) {
            return PRIVATE;
        }
        for (FileVisibility visibility : values()) {
            if (visibility.code.equalsIgnoreCase(code)) {
                return visibility;
            }
        }
        return PRIVATE;
    }
}
