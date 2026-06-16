package com.campus.wall.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IP工具类
 */
public final class IpUtil {

    private IpUtil() {}

    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);

    private static final String[] FORWARDED_IP_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
    };
    private static final String FORWARDED_HEADER = "Forwarded";

    private static volatile boolean trustForwardedHeaders = false;
    private static volatile List<CidrMatcher> trustedProxyMatchers = List.of();

    /**
     * 配置代理信任策略
     */
    public static void configure(boolean trustForwardedHeadersEnabled, List<String> trustedProxies) {
        trustForwardedHeaders = trustForwardedHeadersEnabled;
        trustedProxyMatchers = parseTrustedProxies(trustedProxies);
    }

    /**
     * 获取客户端真实IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (shouldTrustForwardedHeaders(remoteAddr)) {
            String forwardedIp = resolveForwardedIp(request);
            if (isValidIpLiteral(forwardedIp)) {
                return forwardedIp;
            }
        }
        return remoteAddr;
    }

    private static boolean shouldTrustForwardedHeaders(String remoteAddr) {
        if (!trustForwardedHeaders) {
            return false;
        }
        return isTrustedProxy(remoteAddr);
    }

    private static boolean isTrustedProxy(String ip) {
        if (!isValidIpLiteral(ip)) {
            return false;
        }
        List<CidrMatcher> matchers = trustedProxyMatchers;
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }
        for (CidrMatcher matcher : matchers) {
            if (matcher.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveForwardedIp(HttpServletRequest request) {
        String fromForwarded = resolveIpFromForwardedHeader(request.getHeader(FORWARDED_HEADER));
        if (isValidIpLiteral(fromForwarded)) {
            return fromForwarded;
        }
        for (String header : FORWARDED_IP_HEADERS) {
            String candidate = resolveIpFromHeaderValue(request.getHeader(header));
            if (isValidIpLiteral(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String resolveIpFromHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String[] segments = value.split(",");
        for (String segment : segments) {
            String candidate = normalizeIp(segment);
            if (isValidIpLiteral(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String resolveIpFromForwardedHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String[] entries = value.split(",");
        for (String entry : entries) {
            String[] attrs = entry.split(";");
            for (String attr : attrs) {
                String trimmed = attr.trim();
                if (!startsWithIgnoreCase(trimmed, "for=")) {
                    continue;
                }
                String raw = trimmed.substring(4).trim();
                if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                String candidate = normalizeIp(raw);
                if (isValidIpLiteral(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        if (value == null || prefix == null) {
            return false;
        }
        if (value.length() < prefix.length()) {
            return false;
        }
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean isValidIpLiteral(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
            return false;
        }
        try {
            InetAddress.getByName(ip.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<CidrMatcher> parseTrustedProxies(List<String> trustedProxies) {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return List.of();
        }
        List<CidrMatcher> matchers = new ArrayList<>();
        for (String item : trustedProxies) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            try {
                matchers.add(CidrMatcher.parse(item.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("忽略非法 trusted proxy 配置: {}", item);
            }
        }
        return List.copyOf(matchers);
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

    private static final class CidrMatcher {
        private final byte[] networkBytes;
        private final int prefixLength;
        private final int addressBits;

        private CidrMatcher(byte[] networkBytes, int prefixLength) {
            this.networkBytes = networkBytes;
            this.prefixLength = prefixLength;
            this.addressBits = networkBytes.length * 8;
        }

        private static CidrMatcher parse(String expression) {
            String source = expression == null ? "" : expression.trim();
            if (!StringUtils.hasText(source)) {
                throw new IllegalArgumentException("empty expression");
            }
            String ipPart = source;
            Integer prefix = null;
            int slash = source.indexOf('/');
            if (slash >= 0) {
                ipPart = source.substring(0, slash).trim();
                String prefixPart = source.substring(slash + 1).trim();
                if (!StringUtils.hasText(prefixPart)) {
                    throw new IllegalArgumentException("invalid cidr");
                }
                prefix = Integer.parseInt(prefixPart);
            }
            String normalizedIp = normalizeIp(ipPart);
            try {
                InetAddress inetAddress = InetAddress.getByName(normalizedIp);
                byte[] address = inetAddress.getAddress();
                int bits = address.length * 8;
                int finalPrefix = prefix == null ? bits : prefix;
                if (finalPrefix < 0 || finalPrefix > bits) {
                    throw new IllegalArgumentException("invalid prefix");
                }
                return new CidrMatcher(maskAddress(address, finalPrefix), finalPrefix);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid address", e);
            }
        }

        private boolean matches(String ip) {
            if (!StringUtils.hasText(ip)) {
                return false;
            }
            try {
                InetAddress inetAddress = InetAddress.getByName(normalizeIp(ip));
                byte[] candidate = inetAddress.getAddress();
                if (candidate.length * 8 != addressBits) {
                    return false;
                }
                byte[] masked = maskAddress(candidate, prefixLength);
                return Arrays.equals(networkBytes, masked);
            } catch (Exception e) {
                return false;
            }
        }

        private static byte[] maskAddress(byte[] address, int prefixLength) {
            byte[] masked = Arrays.copyOf(address, address.length);
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            if (fullBytes < masked.length) {
                if (remainingBits > 0) {
                    int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                    masked[fullBytes] = (byte) (masked[fullBytes] & mask);
                    fullBytes++;
                }
                for (int i = fullBytes; i < masked.length; i++) {
                    masked[i] = 0;
                }
            }
            return masked;
        }
    }
}
