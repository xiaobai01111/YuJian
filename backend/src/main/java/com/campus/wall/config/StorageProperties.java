package com.campus.wall.config;

import com.campus.wall.enums.file.StorageProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * 主存储提供者
     */
    private StorageProviderType primaryProvider = StorageProviderType.LOCAL;

    /**
     * 失败兜底存储提供者
     */
    private StorageProviderType fallbackProvider = StorageProviderType.LOCAL;

    /**
     * 本地存储路径
     */
    private String localPath = "uploads";

    /**
     * 本地公开访问路径前缀
     */
    private String localUrlPrefix = "/uploads";

    /**
     * 私有文件预览链接有效期（秒）
     */
    private long privateUrlTtlSeconds = 600;

    /**
     * 私有链接签名密钥
     */
    private String signingSecret;
}
