package com.campus.wall.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * MinIO 配置
 */
@Data
@Configuration
@ConditionalOnExpression("'${app.storage.primary-provider:LOCAL}' == 'MINIO' || '${app.storage.fallback-provider:LOCAL}' == 'MINIO'")
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint = "http://localhost:9000";
    private String accessKey;
    private String secretKey;
    private String bucketName = "campus-wall";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
