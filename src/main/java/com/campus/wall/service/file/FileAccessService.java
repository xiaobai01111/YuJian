package com.campus.wall.service.file;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.file.FileRecordMapper;
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

    private static final String TARGET_TYPE_ID_CARD = FileTargetType.ID_CARD.getCode();

    private final StorageProviderRegistry storageProviderRegistry;
    private final StorageProperties storageProperties;
    private final FileRecordMapper fileRecordMapper;

    public String buildAccessUrl(FileRecord record) {
        if (record == null) {
            return null;
        }
        FileVisibility visibility = FileVisibility.fromCode(record.getVisibility());
        String publicKey = record.getPublicKey();
        if (!StringUtils.hasText(publicKey)) {
            return null;
        }
        if (visibility == FileVisibility.PRIVATE) {
            return buildSignedPreviewUrl(publicKey, resolveSignedBindUserId(record));
        }
        return buildPublicPreviewUrl(publicKey);
    }

    public StorageProvider getProvider(String providerCode) {
        StorageProviderType type = StorageProviderType.fromCode(providerCode);
        return storageProviderRegistry.getProvider(type);
    }

    public String buildSignedPreviewUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            return null;
        }
        return buildSignedPreviewUrl(record.getPublicKey(), resolveSignedBindUserId(record));
    }

    public String buildSignedPreviewUrl(String publicKey) {
        return buildSignedPreviewUrl(publicKey, null);
    }

    public String buildSignedPreviewUrl(String publicKey, Long bindUserId) {
        if (!StringUtils.hasText(publicKey)) {
            return null;
        }
        long expires = System.currentTimeMillis() / 1000 + storageProperties.getPrivateUrlTtlSeconds();
        String sig = sign(publicKey, expires, bindUserId);
        StringBuilder url = new StringBuilder("/api/v1/files/preview/")
            .append(publicKey)
            .append("?expires=").append(expires);
        if (bindUserId != null) {
            url.append("&uid=").append(bindUserId);
        }
        url.append("&sig=").append(sig);
        return url.toString();
    }

    public String buildPublicPreviewUrl(String publicKey) {
        if (!StringUtils.hasText(publicKey)) {
            return null;
        }
        return "/api/v1/files/preview/" + publicKey;
    }

    public void verifySignature(String publicKey, long expires, String sig) {
        verifySignature(publicKey, expires, sig, null);
    }

    public void verifySignature(String publicKey, long expires, String sig, Long bindUserId) {
        long now = System.currentTimeMillis() / 1000;
        if (expires <= now) {
            throw new BusinessException(ResultCode.FORBIDDEN, "链接已过期");
        }
        String expected = sign(publicKey, expires, bindUserId);
        if (!constantTimeEquals(expected, sig)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无效的访问签名");
        }
    }

    private String sign(String publicKey, long expires, Long bindUserId) {
        String payload = publicKey + ":" + expires + ":" + (bindUserId == null ? "" : bindUserId);
        String secret = storageProperties.getSigningSecret();
        if (!StringUtils.hasText(secret) || "change-me".equalsIgnoreCase(secret)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "签名密钥未配置");
        }
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

    private Long resolveSignedBindUserId(FileRecord record) {
        if (!isVerificationEvidence(record)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            return Long.valueOf(loginId.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isVerificationEvidence(FileRecord record) {
        return record != null
            && StringUtils.hasText(record.getTargetType())
            && TARGET_TYPE_ID_CARD.equalsIgnoreCase(record.getTargetType().trim());
    }
}
