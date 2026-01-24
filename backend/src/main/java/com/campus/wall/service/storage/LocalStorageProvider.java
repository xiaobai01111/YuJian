package com.campus.wall.service.storage;

import com.campus.wall.config.StorageProperties;
import com.campus.wall.enums.file.StorageProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class LocalStorageProvider implements StorageProvider {

    private final StorageProperties storageProperties;

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.LOCAL;
    }

    @Override
    public String store(MultipartFile file, String objectName) throws Exception {
        Path target = resolveSafePath(objectName);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return objectName;
    }

    @Override
    public InputStream open(String path) throws Exception {
        Path target = resolveSafePath(path);
        if (!Files.exists(target)) {
            throw new IOException("Local file not found");
        }
        return new FileInputStream(target.toFile());
    }

    @Override
    public void delete(String path) throws Exception {
        Path target = resolveSafePath(path);
        Files.deleteIfExists(target);
    }

    @Override
    public String buildPublicUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String prefix = storageProperties.getLocalUrlPrefix();
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (prefix.endsWith("/")) {
            return prefix + normalized;
        }
        return prefix + "/" + normalized;
    }

    private Path resolveBasePath() {
        String base = storageProperties.getLocalPath();
        Path basePath = Paths.get(base);
        if (!basePath.isAbsolute()) {
            basePath = Paths.get(System.getProperty("user.dir")).resolve(basePath);
        }
        return basePath.normalize();
    }

    private Path resolveSafePath(String path) throws IOException {
        Path basePath = resolveBasePath();
        Path target = basePath.resolve(path).normalize();
        if (!target.startsWith(basePath)) {
            throw new IOException("Invalid local file path");
        }
        return target;
    }
}
