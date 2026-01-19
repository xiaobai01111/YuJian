package com.campus.wall.service.file;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.service.storage.StorageProvider;
import com.campus.wall.service.storage.StorageProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class FileAccessService {

    private final StorageProviderRegistry storageProviderRegistry;
    private final StorageProperties storageProperties;

    public String buildAccessUrl(FileRecord record) {
        if (record == null) {
            return null;
        }
        FileVisibility visibility = FileVisibility.fromCode(record.getVisibility());
        if (isImage(record.getMimeType())) {
            return visibility == FileVisibility.PRIVATE
                    ? buildSignedPreviewUrl(record.getId())
                    : buildPublicPreviewUrl(record.getId());
        }
        StorageProvider provider = getProvider(record.getStorageProvider());
        if (provider == null) {
            return null;
        }
        if (visibility == FileVisibility.PRIVATE) {
            return buildSignedPreviewUrl(record.getId());
        }
        return provider.buildPublicUrl(record.getPath());
    }

    public StorageProvider getProvider(String providerCode) {
        StorageProviderType type = StorageProviderType.fromCode(providerCode);
        return storageProviderRegistry.getProvider(type);
    }

    public String buildSignedPreviewUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }
        long expires = System.currentTimeMillis() / 1000 + storageProperties.getPrivateUrlTtlSeconds();
        String sig = sign(fileId, expires);
        return "/api/v1/files/preview/" + fileId + "?expires=" + expires + "&sig=" + sig;
    }

    public String buildPublicPreviewUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return "/api/v1/files/preview/" + fileId;
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }

    public void verifySignature(Long fileId, long expires, String sig) {
        long now = System.currentTimeMillis() / 1000;
        if (expires <= now) {
            throw new BusinessException(ResultCode.FORBIDDEN, "链接已过期");
        }
        String expected = sign(fileId, expires);
        if (!constantTimeEquals(expected, sig)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无效的访问签名");
        }
    }

    private String sign(Long fileId, long expires) {
        String payload = fileId + ":" + expires;
        String secret = storageProperties.getSigningSecret();
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
