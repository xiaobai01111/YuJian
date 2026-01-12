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

        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }
}
