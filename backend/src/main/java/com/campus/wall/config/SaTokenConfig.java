package com.campus.wall.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.service.system.BlocklistService;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.util.IpUtil;
import com.campus.wall.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SaTokenConfig.class);
    private static final String BLOCKLIST_TYPE_IP = "IP";
    private static final String BLOCKLIST_TYPE_USER = "USER";
    private static final String BLOCKLIST_TYPE_DEVICE = "DEVICE";
    private static final String[] DEVICE_ID_HEADERS = {
            "X-Device-Id", "X-Device-ID", "X-Client-Id", "X-Client-ID", "Device-Id", "Device-ID"
    };

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    private final PermissionService permissionService;
    private final BlocklistService blocklistService;
    private final SecurityProperties securityProperties;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 跳过 OPTIONS 预检请求
            String method = SaHolder.getRequest().getMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return;
            }
            String uri = SaHolder.getRequest().getRequestPath();
            if (!uri.startsWith("/api/")) {
                return;
            }
            checkBlocklist(method, uri);

            SysApiPermission rule = permissionService.getApiPermissionByUrl(uri, method);
            if (rule == null) {
                applyDefaultPolicy();
                return;
            }

            String requiredPerm = rule.getPermission();
            if (isPublicPermission(requiredPerm)) {
                return;
            }

            StpUtil.checkLogin();
            if (isLoginOnlyPermission(requiredPerm)) {
                return;
            }

            if (StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey())) {
                return;
            }

            Long userId = StpUtil.getLoginIdAsLong();
            if (!permissionService.hasPermission(userId, requiredPerm)) {
                log.warn("用户 {} 访问 {} {} 权限不足，需要权限: {}", userId, method, uri, requiredPerm);
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        })).addPathPatterns("/**");
    }

    private void checkBlocklist(String method, String uri) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }

        String ip = IpUtil.getClientIp(request);
        if (StringUtils.hasText(ip) && blocklistService.isBlocked(BLOCKLIST_TYPE_IP, ip)) {
            log.warn("请求命中阻止名单(type=IP): ip={}, method={}, uri={}", ip, method, uri);
            throw new BusinessException(ResultCode.FORBIDDEN, "访问已被阻止");
        }

        String deviceId = resolveDeviceId(request);
        if (StringUtils.hasText(deviceId) && blocklistService.isBlocked(BLOCKLIST_TYPE_DEVICE, deviceId)) {
            log.warn("请求命中阻止名单(type=DEVICE): deviceId={}, method={}, uri={}", deviceId, method, uri);
            throw new BusinessException(ResultCode.FORBIDDEN, "访问已被阻止");
        }

        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            if (blocklistService.isBlocked(BLOCKLIST_TYPE_USER, String.valueOf(userId))) {
                StpUtil.logout();
                log.warn("请求命中阻止名单(type=USER): userId={}, method={}, uri={}", userId, method, uri);
                throw new BusinessException(ResultCode.USER_BANNED);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String resolveDeviceId(HttpServletRequest request) {
        for (String header : DEVICE_ID_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = splitAndTrim(allowedOrigins);
        String[] patterns = splitAndTrim(allowedOriginPatterns);
        if (origins.length == 0 && patterns.length == 0) {
            log.warn("CORS 未配置允许来源，已默认拒绝跨域访问");
            return;
        }

        var registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Trace-Id")
                .maxAge(3600);

        if (origins.length > 0) {
            registration.allowedOrigins(origins);
        }
        if (patterns.length > 0) {
            registration.allowedOriginPatterns(patterns);
        }
        boolean hasWildcard = containsWildcard(origins) || containsWildcard(patterns);
        boolean allowCredentials = !hasWildcard;
        if (hasWildcard) {
            log.warn("CORS 配置包含通配符，已禁用 allowCredentials 以降低风险");
        }
        registration.allowCredentials(allowCredentials);
    }

    private String[] splitAndTrim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toArray(String[]::new);
    }

    private boolean containsWildcard(String[] values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.contains("*")) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicPermission(String permission) {
        if (permission == null) {
            return false;
        }
        return permission.equalsIgnoreCase(securityProperties.getApiPublicPermissionKey());
    }

    private boolean isLoginOnlyPermission(String permission) {
        if (permission == null) {
            return false;
        }
        return permission.equalsIgnoreCase(securityProperties.getApiLoginPermissionKey());
    }

    private void applyDefaultPolicy() {
        String mode = securityProperties.getApiDefaultMode();
        if (!permissionService.hasApiPermissions()) {
            mode = securityProperties.getApiBootstrapMode();
        }
        if (mode == null) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        switch (mode.trim().toLowerCase()) {
            case "public":
                return;
            case "login":
                StpUtil.checkLogin();
                return;
            case "deny":
                throw new BusinessException(ResultCode.FORBIDDEN);
            default:
                throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
