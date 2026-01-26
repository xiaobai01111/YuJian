package com.campus.wall.service.storage;

import com.campus.wall.config.MinioConfig;
import com.campus.wall.enums.file.StorageProviderType;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnBean(MinioClient.class)
@RequiredArgsConstructor
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.MINIO;
    }

    @Override
    public String store(MultipartFile file, String objectName) throws Exception {
        ensureBucketExists();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return objectName;
    }

    @Override
    public InputStream open(String path) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(path)
                .build());
    }

    @Override
    public void delete(String path) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(path)
                .build());
    }

    @Override
    public String buildPublicUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String endpoint = minioConfig.getEndpoint();
        String bucket = minioConfig.getBucketName();
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + path;
    }

    private void ensureBucketExists() throws Exception {
        if (bucketReady.get()) {
            return;
        }
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
        }
        bucketReady.set(true);
    }
}
