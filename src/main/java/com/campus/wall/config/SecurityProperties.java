package com.campus.wall.config;

import lombok.Data;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.campus.wall.util.SecurityUtil;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    /**
     * 超级管理员角色标识
     */
    private String superAdminRoleKey = "admin";

    /**
     * API 权限：公开访问标识
     */
    private String apiPublicPermissionKey = "public";

    /**
     * API 权限：仅登录标识
     */
    private String apiLoginPermissionKey = "login";

    /**
     * API 默认策略（public/login/deny）
     */
    private String apiDefaultMode = "deny";

    /**
     * API 启动引导策略（public/login/deny）
     */
    private String apiBootstrapMode = "deny";

    /**
     * 加密密钥（SMTP密码等敏感信息加密用）
     * 支持：环境变量 CAMPUS_CRYPTO_KEY 或 配置文件 app.security.crypto-key
     * 生产环境必须配置，开发环境可使用默认值
     */
    private String cryptoKey = "${CAMPUS_CRYPTO_KEY:}";
    
    /**
     * 启动时检查关键密钥
     */
    private boolean failFastSecrets = true;

    /**
     * 启动时检查权限规则
     */
    private boolean failFastPermissions = true;

    /**
     * 限流组件异常时是否拒绝请求（fail-closed）
     */
    private boolean rateLimitFailClosed = true;

    /**
     * 用户权限缓存过期时间（秒）
     */
    private long permissionCacheTtlSeconds = 1800;

    /**
     * 用户权限缓存最大容量
     */
    private long permissionCacheMaxSize = 20000;

    /**
     * 是否允许匿名访问健康检查端点（/actuator/health）
     */
    private boolean actuatorAllowAnonymousHealth = false;

    /**
     * Actuator 是否要求超级管理员角色
     */
    private boolean actuatorRequireSuperAdmin = true;

    /**
     * Refresh Token Cookie 是否强制设置 Secure
     * 默认针对开发/未启 TLS 的环境不开启，正式部署时可设置为 true
     */
    private boolean refreshCookieForceSecure = false;

    @PostConstruct
    public void init() {
        SecurityUtil.setSuperAdminRoleKey(superAdminRoleKey);
        // 注入加密密钥到 CryptoUtils
        com.campus.wall.util.CryptoUtils.setInjectedKey(cryptoKey);
    }
}
