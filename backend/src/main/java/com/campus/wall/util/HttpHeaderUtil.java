package com.campus.wall.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应头安全工具
 */
public final class HttpHeaderUtil {

    private HttpHeaderUtil() {
    }

    public static String buildContentDisposition(String filename, boolean attachment) {
        String safeName = sanitizeFilename(filename);
        String asciiName = toAsciiFallback(safeName);
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        String type = attachment ? "attachment" : "inline";
        return type + "; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encoded;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String sanitized = filename.replace("\r", "").replace("\n", "");
        sanitized = sanitized.replace("/", "_").replace("\\", "_").replace("\"", "");
        sanitized = sanitized.replaceAll("\\.\\.+", ".");
        if (sanitized.isBlank() || ".".equals(sanitized)) {
            return "file";
        }
        return sanitized;
    }

    private static String toAsciiFallback(String safeName) {
        String name = safeName;
        String extension = "";
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < safeName.length() - 1) {
            name = safeName.substring(0, dotIndex);
            extension = safeName.substring(dotIndex);
        }
        String asciiBase = name.replaceAll("[^A-Za-z0-9._-]", "_");
        asciiBase = asciiBase.replaceAll("_+", "_");
        if (asciiBase.isBlank()) {
            asciiBase = "file";
        }
        String asciiExt = extension.replaceAll("[^A-Za-z0-9.]", "");
        return asciiBase + asciiExt;
    }
}
