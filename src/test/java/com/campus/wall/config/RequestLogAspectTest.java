package com.campus.wall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogAspectTest {

    private RequestLogAspect aspect;
    private Method sanitizeQueryStringMethod;
    private Method sanitizeMapMethod;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new RequestLogAspect(new ObjectMapper());
        sanitizeQueryStringMethod = RequestLogAspect.class.getDeclaredMethod("sanitizeQueryString", String.class);
        sanitizeQueryStringMethod.setAccessible(true);
        sanitizeMapMethod = RequestLogAspect.class.getDeclaredMethod("sanitizeMap", Map.class);
        sanitizeMapMethod.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sanitizeQueryString_masksSigAndStudentId() throws Exception {
        Map<String, Object> sanitized = (Map<String, Object>) sanitizeQueryStringMethod.invoke(
            aspect,
            "sig=abcdef&signature=raw-signature&studentId=20240001&page=1"
        );

        assertThat(sanitized.get("sig")).isEqualTo("***");
        assertThat(sanitized.get("signature")).isEqualTo("***");
        assertThat(sanitized.get("studentId")).isEqualTo("***");
        assertThat(sanitized.get("page")).isEqualTo("1");
    }

    @SuppressWarnings("unchecked")
    @Test
    void sanitizeMap_masksSigAndStudentId() throws Exception {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("sig", "abcdef");
        raw.put("studentId", "20240001");
        raw.put("nickname", "alice");

        Map<String, Object> sanitized = (Map<String, Object>) sanitizeMapMethod.invoke(aspect, raw);

        assertThat(sanitized.get("sig")).isEqualTo("***");
        assertThat(sanitized.get("studentId")).isEqualTo("***");
        assertThat(sanitized.get("nickname")).isEqualTo("alice");
    }
}
