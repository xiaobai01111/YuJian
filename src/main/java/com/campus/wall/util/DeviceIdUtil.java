package com.campus.wall.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 设备标识规范化与格式校验。
 */
public final class DeviceIdUtil {

    private static final int DEVICE_ID_MIN_LENGTH = 6;
    private static final int DEVICE_ID_MAX_LENGTH = 64;
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{5,63}$");

    private DeviceIdUtil() {
    }

    public static String normalizeOrNull(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim();
        if (normalized.length() < DEVICE_ID_MIN_LENGTH || normalized.length() > DEVICE_ID_MAX_LENGTH) {
            return null;
        }
        if (!DEVICE_ID_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }
}
