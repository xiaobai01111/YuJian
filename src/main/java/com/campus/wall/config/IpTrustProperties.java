package com.campus.wall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IP 与代理信任配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ip")
public class IpTrustProperties {

    /**
     * 是否信任 X-Forwarded-For / Forwarded 等转发头
     */
    private boolean trustForwardedHeaders = false;

    /**
     * 可信代理网段（CIDR 或单IP）
     */
    private List<String> trustedProxies = List.of();
}
