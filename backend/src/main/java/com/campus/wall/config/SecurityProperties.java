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

    @PostConstruct
    public void init() {
        SecurityUtil.setSuperAdminRoleKey(superAdminRoleKey);
    }
}
