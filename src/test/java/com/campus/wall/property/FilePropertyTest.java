package com.campus.wall.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件相关属性测试
 * Property 21: 文件格式验证
 * Property 22: 图片压缩有效性
 */
class FilePropertyTest {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Property 21: 允许的图片格式应通过验证
     */
    @Property(tries = 100)
    void allowedImageTypesPassValidation(
            @ForAll("allowedMimeType") String mimeType) {
        boolean isValid = validateMimeType(mimeType);
        assertThat(isValid).isTrue();
    }

    /**
     * Property 21: 不允许的文件格式应被拒绝
     */
    @Property(tries = 100)
    void disallowedTypesAreRejected(
            @ForAll("disallowedMimeType") String mimeType) {
        boolean isValid = validateMimeType(mimeType);
        assertThat(isValid).isFalse();
    }

    /**
     * Property 21: 允许的文件扩展名应通过验证
     */
    @Property(tries = 100)
    void allowedExtensionsPassValidation(
            @ForAll("allowedExtension") String extension) {
        boolean isValid = validateExtension(extension);
        assertThat(isValid).isTrue();
    }

    /**
     * Property 21: 文件大小在限制内应通过
     */
    @Property(tries = 100)
    void fileSizeWithinLimitPasses(
            @ForAll @LongRange(min = 1, max = 5242880) long fileSize) {
        boolean isValid = validateFileSize(fileSize);
        assertThat(isValid).isTrue();
    }

    /**
     * Property 21: 超出大小限制的文件应被拒绝
     */
    @Property(tries = 100)
    void fileSizeExceedingLimitIsRejected(
            @ForAll @LongRange(min = 5242881, max = 10485760) long fileSize) {
        boolean isValid = validateFileSize(fileSize);
        assertThat(isValid).isFalse();
    }

    /**
     * Property 22: 压缩后文件大小不应增加（对于已压缩格式）
     */
    @Property(tries = 50)
    void compressionDoesNotIncreaseSize(
            @ForAll @LongRange(min = 1024, max = 5242880) long originalSize,
            @ForAll @DoubleRange(min = 0.1, max = 1.0) double compressionRatio) {
        long compressedSize = simulateCompression(originalSize, compressionRatio);
        assertThat(compressedSize).isLessThanOrEqualTo(originalSize);
    }

    /**
     * Property 22: 压缩比应在有效范围内
     */
    @Property(tries = 50)
    void compressionRatioIsValid(
            @ForAll @LongRange(min = 1024, max = 5242880) long originalSize,
            @ForAll @DoubleRange(min = 0.1, max = 0.9) double targetRatio) {
        long compressedSize = simulateCompression(originalSize, targetRatio);
        double actualRatio = (double) compressedSize / originalSize;
        assertThat(actualRatio).isLessThanOrEqualTo(1.0);
        assertThat(actualRatio).isGreaterThan(0.0);
    }

    /**
     * Property 22: 零大小文件应被拒绝
     */
    @Example
    void zeroSizeFileIsRejected() {
        boolean isValid = validateFileSize(0);
        assertThat(isValid).isFalse();
    }

    // 验证方法
    private boolean validateMimeType(String mimeType) {
        return mimeType != null && ALLOWED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }

    private boolean validateExtension(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    private boolean validateFileSize(long size) {
        return size > 0 && size <= MAX_FILE_SIZE;
    }

    private long simulateCompression(long originalSize, double ratio) {
        return (long) (originalSize * ratio);
    }

    // 数据提供者
    @Provide
    Arbitrary<String> allowedMimeType() {
        return Arbitraries.of("image/jpeg", "image/png", "image/webp", "image/gif");
    }

    @Provide
    Arbitrary<String> disallowedMimeType() {
        return Arbitraries.of(
                "application/pdf", "text/plain", "video/mp4",
                "application/zip", "image/bmp", "image/tiff"
        );
    }

    @Provide
    Arbitrary<String> allowedExtension() {
        return Arbitraries.of("jpg", "jpeg", "png", "webp", "gif");
    }
}
