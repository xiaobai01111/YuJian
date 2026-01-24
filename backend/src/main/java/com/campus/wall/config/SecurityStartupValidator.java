package com.campus.wall.config;

import com.campus.wall.enums.file.StorageProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityStartupValidator implements ApplicationRunner {

    private final StorageProperties storageProperties;
    private final ObjectProvider<MinioConfig> minioConfigProvider;
    private final SecurityProperties securityProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isFailFastSecrets()) {
            log.warn("Secret validation is disabled (app.security.fail-fast-secrets=false).");
            return;
        }
        validateSigningSecret();
        validateMinioSecrets();
    }

    private void validateSigningSecret() {
        String secret = storageProperties.getSigningSecret();
        if (!StringUtils.hasText(secret) || "change-me".equalsIgnoreCase(secret)) {
            throw new IllegalStateException("Storage signing secret is missing or unsafe.");
        }
    }

    private void validateMinioSecrets() {
        boolean minioRequired = storageProperties.getPrimaryProvider() == StorageProviderType.MINIO
                || storageProperties.getFallbackProvider() == StorageProviderType.MINIO;
        if (!minioRequired) {
            return;
        }
        MinioConfig minioConfig = minioConfigProvider.getIfAvailable();
        if (minioConfig == null) {
            throw new IllegalStateException("MinIO 未启用，但存储提供者配置为 MINIO。");
        }
        if (!StringUtils.hasText(minioConfig.getAccessKey()) || !StringUtils.hasText(minioConfig.getSecretKey())) {
            throw new IllegalStateException("MinIO accessKey/secretKey is required.");
        }
    }
}
