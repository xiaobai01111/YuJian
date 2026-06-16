package com.campus.wall.util;

import cn.hutool.crypto.SecureUtil;

import java.util.Locale;

/**
 * 敏感字段工具：统一处理加密与哈希。
 */
public final class SensitiveFieldUtil {

    private static final String ENCRYPTED_PREFIX = "ENCv1:";

    private SensitiveFieldUtil() {
    }

    public static String encryptIfNeeded(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (isEncrypted(value)) {
            return value;
        }
        return ENCRYPTED_PREFIX + CryptoUtils.encrypt(value);
    }

    public static String decryptIfNeeded(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!isEncrypted(value)) {
            return value;
        }
        String ciphertext = value.substring(ENCRYPTED_PREFIX.length());
        return CryptoUtils.decrypt(ciphertext);
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    public static String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static String normalizePhone(String phone) {
        return trimToNull(phone);
    }

    public static String normalizeStudentId(String studentId) {
        return trimToNull(studentId);
    }

    public static String hashEmail(String email) {
        String normalized = normalizeEmail(email);
        return normalized == null ? null : SecureUtil.sha256(normalized);
    }

    public static String hashPhone(String phone) {
        String normalized = normalizePhone(phone);
        return normalized == null ? null : SecureUtil.sha256(normalized);
    }

    public static String hashStudentId(String studentId) {
        String normalized = normalizeStudentId(studentId);
        return normalized == null ? null : SecureUtil.sha256(normalized);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
