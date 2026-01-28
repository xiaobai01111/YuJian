package com.campus.wall.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密工具类 - 使用 AES-GCM 加密算法
 * 
 * 安全说明：
 * - 生产环境必须配置加密密钥（环境变量或配置文件）
 * - 使用默认密钥时会输出警告日志
 * 
 * 密钥配置方式（优先级从高到低）：
 * 1. 环境变量: CAMPUS_CRYPTO_KEY
 * 2. 配置文件: app.security.crypto-key
 * 3. 默认密钥（仅开发环境）
 */
@Slf4j
public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * 环境变量名：加密密钥
     */
    public static final String ENV_CRYPTO_KEY = "CAMPUS_CRYPTO_KEY";
    
    /**
     * 默认密钥（仅用于开发环境，生产环境必须配置）
     */
    private static final String DEFAULT_KEY = "campus-wall-config-key-32bytes!";
    
    /**
     * Spring 注入的密钥（来自配置文件）
     */
    private static volatile String injectedKey = null;
    
    /**
     * 是否已警告过使用默认密钥
     */
    private static volatile boolean defaultKeyWarned = false;

    private CryptoUtils() {}
    
    /**
     * 由 Spring 初始化时注入配置的密钥
     */
    public static void setInjectedKey(String key) {
        if (key != null && !key.isBlank() && !key.startsWith("${")) {
            injectedKey = key;
        }
    }

    /**
     * 加密字符串
     */
    public static String encrypt(String plaintext, String secretKey) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = generateIv();
            SecretKeySpec keySpec = deriveKey(secretKey);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密字符串
     */
    public static String decrypt(String ciphertext, String secretKey) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKeySpec keySpec = deriveKey(secretKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 使用配置的密钥加密（优先从环境变量读取）
     */
    public static String encrypt(String plaintext) {
        return encrypt(plaintext, getConfiguredKey());
    }

    /**
     * 使用配置的密钥解密（优先从环境变量读取）
     */
    public static String decrypt(String ciphertext) {
        return decrypt(ciphertext, getConfiguredKey());
    }
    
    /**
     * 获取配置的加密密钥
     * 优先级：环境变量 > 配置文件 > 默认密钥
     */
    private static String getConfiguredKey() {
        // 1. 优先读取环境变量
        String envKey = System.getenv(ENV_CRYPTO_KEY);
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        // 2. 其次读取配置文件（由Spring注入）
        if (injectedKey != null && !injectedKey.isBlank()) {
            return injectedKey;
        }
        // 3. 使用默认密钥时输出警告
        if (!defaultKeyWarned) {
            defaultKeyWarned = true;
            log.warn("[安全警告] 正在使用默认加密密钥，生产环境请配置: 环境变量 {} 或 配置文件 app.security.crypto-key", ENV_CRYPTO_KEY);
        }
        return DEFAULT_KEY;
    }
    
    /**
     * 检查是否使用了默认密钥（用于启动时安全检查）
     */
    public static boolean isUsingDefaultKey() {
        // 检查环境变量
        String envKey = System.getenv(ENV_CRYPTO_KEY);
        if (envKey != null && !envKey.isBlank()) {
            return false;
        }
        // 检查配置文件注入的密钥
        if (injectedKey != null && !injectedKey.isBlank()) {
            return false;
        }
        return true;
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static SecretKeySpec deriveKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(hash, 32), "AES");
    }
}
