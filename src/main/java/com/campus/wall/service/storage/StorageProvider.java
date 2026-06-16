package com.campus.wall.service.storage;

import com.campus.wall.enums.file.StorageProviderType;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageProvider {

    StorageProviderType getType();

    String store(MultipartFile file, String objectName) throws Exception;

    InputStream open(String path) throws Exception;

    void delete(String path) throws Exception;

    String buildPublicUrl(String path);
}
