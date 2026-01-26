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
    private String apiDefaultMode = "login";

    /**
     * API 启动引导策略（public/login/deny）
     */
    private String apiBootstrapMode = "login";

    /**
     * 启动时检查关键密钥
     */
    private boolean failFastSecrets = true;

    /**
     * 启动时检查权限规则
     */
    private boolean failFastPermissions = true;

    /**
     * 用户权限缓存过期时间（秒）
     */
    private long permissionCacheTtlSeconds = 1800;

    /**
     * 用户权限缓存最大容量
     */
    private long permissionCacheMaxSize = 20000;

    @PostConstruct
    public void init() {
        SecurityUtil.setSuperAdminRoleKey(superAdminRoleKey);
    }
}
