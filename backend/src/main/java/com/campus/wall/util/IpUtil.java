package com.campus.wall.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP工具类
 */
public final class IpUtil {

    private IpUtil() {}

    private static final String[] IP_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 获取客户端真实IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        return normalizeIp(ip);
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return ip;
        }
        String trimmed = ip.trim();
        // 多个代理时取第一个
        if (trimmed.contains(",")) {
            trimmed = trimmed.split(",")[0].trim();
        }
        // 处理 IPv6 带端口的格式：[::1]:8080
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            trimmed = trimmed.substring(1, trimmed.indexOf(']'));
        } else {
            // 处理 IPv4 带端口的格式：1.2.3.4:8080
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > -1 && trimmed.indexOf(':', colonIndex + 1) == -1) {
                trimmed = trimmed.substring(0, colonIndex);
            }
        }
        // 处理 IPv4 映射 IPv6
        if (trimmed.startsWith("::ffff:")) {
            trimmed = trimmed.substring(7);
        }
        // 规范化 IPv6 显示
        try {
            return java.net.InetAddress.getByName(trimmed).getHostAddress();
        } catch (Exception e) {
            return trimmed;
        }
    }
}
