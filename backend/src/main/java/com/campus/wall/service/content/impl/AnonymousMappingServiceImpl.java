package com.campus.wall.service.content.impl;

import com.campus.wall.service.content.AnonymousMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 匿名映射服务实现
 * 使用 AES-GCM 加密算法
 * 
 * 注意：生产环境应接入 KMS（如阿里云 KMS、AWS KMS）
 * 当前使用本地密钥实现
 */
@Slf4j
@Service
public class AnonymousMappingServiceImpl implements AnonymousMappingService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${kms.master-key:campus-wall-default-key-32b}")
    private String masterKey;

    // 缓存匿名标签映射（帖子ID -> 用户ID -> 标签）
    private final Map<Long, Map<Long, String>> anonymousTagCache = new ConcurrentHashMap<>();
    private static final String[] ANONYMOUS_TAGS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

    @Override
    public String encryptUserId(Long userId, String context) {
        try {
            byte[] iv = generateIv();
            SecretKeySpec keySpec = deriveKey(context);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            byte[] plaintext = userId.toString().getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // IV + Ciphertext
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            log.error("加密用户ID失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    @Override
    public Long decryptUserId(String encryptedId, String context) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(encryptedId);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[data.length - GCM_IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(data, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            SecretKeySpec keySpec = deriveKey(context);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return Long.parseLong(new String(plaintext, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("解密用户ID失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    @Override
    public String generateAnonymousTag(Long userId, Long postId) {
        return anonymousTagCache
                .computeIfAbsent(postId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> {
                    int index = anonymousTagCache.get(postId).size();
                    if (index < ANONYMOUS_TAGS.length) {
                        return "匿名用户" + ANONYMOUS_TAGS[index];
                    } else {
                        return "匿名用户" + (index + 1);
                    }
                });
    }

    @Override
    public boolean isSameUser(String encryptedId1, String encryptedId2, String context) {
        try {
            Long userId1 = decryptUserId(encryptedId1, context);
            Long userId2 = decryptUserId(encryptedId2, context);
            return userId1.equals(userId2);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private SecretKeySpec deriveKey(String context) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(masterKey.getBytes(StandardCharsets.UTF_8));
        if (context != null) {
            digest.update(context.getBytes(StandardCharsets.UTF_8));
        }
        byte[] keyBytes = digest.digest();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
