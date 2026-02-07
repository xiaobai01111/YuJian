package com.campus.wall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 内容审核与文件安全扫描配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.content-moderation")
public class ContentModerationProperties {

    /**
     * 是否启用图片内容审核
     */
    private boolean imageAuditEnabled = true;

    /**
     * 图片审核失败时是否拒绝（fail-closed）
     */
    private boolean imageAuditFailClosed = true;

    /**
     * 图片审核服务 API 地址（POST）
     */
    private String imageAuditApiUrl;

    /**
     * 图片审核服务令牌（可选）
     */
    private String imageAuditApiToken;

    /**
     * 图片审核超时时间（毫秒）
     */
    private int imageAuditTimeoutMs = 5000;

    /**
     * 是否启用病毒扫描
     */
    private boolean virusScanEnabled = true;

    /**
     * 病毒扫描失败时是否拒绝（fail-closed）
     */
    private boolean virusScanFailClosed = true;

    /**
     * ClamAV 服务地址
     */
    private String clamavHost = "127.0.0.1";

    /**
     * ClamAV 服务端口
     */
    private int clamavPort = 3310;

    /**
     * ClamAV 连接超时（毫秒）
     */
    private int clamavConnectTimeoutMs = 2000;

    /**
     * ClamAV 读超时（毫秒）
     */
    private int clamavReadTimeoutMs = 10000;
}
