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
        String normalized = normalize(code);
        if (normalized == null) {
            return PRIVATE;
        }
        for (FileVisibility visibility : values()) {
            if (visibility.code.equalsIgnoreCase(normalized)) {
                return visibility;
            }
        }
        return PRIVATE;
    }

    public static boolean isValidCode(String code) {
        String normalized = normalize(code);
        if (normalized == null) {
            return false;
        }
        for (FileVisibility visibility : values()) {
            if (visibility.code.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
