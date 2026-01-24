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
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        String type = attachment ? "attachment" : "inline";
        return type + "; filename=\"" + safeName + "\"; filename*=UTF-8''" + encoded;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String sanitized = filename.replace("\r", "").replace("\n", "");
        sanitized = sanitized.replace("/", "_").replace("\\", "_").replace("\"", "");
        return sanitized;
    }
}
