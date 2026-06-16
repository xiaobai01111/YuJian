package com.campus.wall.config;

import com.campus.wall.util.IpUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化 IP 工具的代理信任策略
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class IpUtilConfig {

    private final IpTrustProperties ipTrustProperties;

    @PostConstruct
    public void init() {
        IpUtil.configure(
            ipTrustProperties.isTrustForwardedHeaders(),
            ipTrustProperties.getTrustedProxies()
        );
        log.info(
            "IP 代理信任配置已加载: trustForwardedHeaders={}, trustedProxies={}",
            ipTrustProperties.isTrustForwardedHeaders(),
            ipTrustProperties.getTrustedProxies()
        );
    }
}
