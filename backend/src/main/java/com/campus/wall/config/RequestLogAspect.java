package com.campus.wall.config;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.R;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestLogAspect {

    private static final int MAX_VALUE_LENGTH = 200;
    private static final int MAX_COLLECTION_SIZE = 10;
    private static final String MASK = "***";

    private final ObjectMapper objectMapper;

    @Pointcut("within(com.campus.wall.controller..*)")
    @SuppressWarnings("EmptyMethod")
    public void controllerLayer() {
        // pointcut definition
    }

    @Around("controllerLayer()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = request.getRemoteAddr();

        Map<String, Object> params = sanitizeArgs(joinPoint.getArgs());
        if (StringUtils.hasText(query)) {
            params.put("_query", sanitizeQueryString(query));
        }

        long start = System.currentTimeMillis();
        log.info("request {} {} ip={} params={}", method, path, clientIp, params);

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("response {} {} costMs={} result={}", method, path, cost, summarizeResult(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            if (ex instanceof BusinessException businessException) {
                log.warn(
                    "request failed {} {} costMs={} code={} message={}",
                    method,
                    path,
                    cost,
                    businessException.getCode(),
                    truncate(businessException.getMessage())
                );
            } else {
                log.error("request failed {} {} costMs={}", method, path, cost, ex);
            }
            throw ex;
        }
    }

    private Map<String, Object> sanitizeArgs(Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (isSkippable(arg)) {
                continue;
            }
            params.put("arg" + i, sanitizeValue(arg));
        }
        return params;
    }

    private Object summarizeResult(Object result) {
        switch (result) {
            case null -> {
                return null;
            }
            case R<?> r -> {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("code", r.getCode());
                summary.put("message", truncate(r.getMessage()));
                summary.put("success", r.isSuccess());
                return summary;
            }
            case Collection<?> collection -> {
                return "Collection(size=" + collection.size() + ")";
            }
            default -> {
            }
        }
        if (result.getClass().isArray()) {
            return "Array(length=" + java.lang.reflect.Array.getLength(result) + ")";
        }
        if (result instanceof String str) {
            return truncate(str);
        }
        return result.getClass().getSimpleName();
    }

    private boolean isSkippable(Object arg) {
        return arg == null
            || arg instanceof HttpServletRequest
            || arg instanceof HttpServletResponse
            || arg instanceof MultipartFile
            || arg instanceof MultipartFile[]
            || arg instanceof BindingResult
            || arg instanceof InputStream
            || arg instanceof byte[];
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return truncate(str);
        }
        if (isSimpleType(value)) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return sanitizeCollection(collection);
        }
        if (value.getClass().isArray()) {
            return sanitizeArray(value);
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<>() {});
            return sanitizeMap(map);
        } catch (IllegalArgumentException ex) {
            return value.getClass().getSimpleName();
        }
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= MAX_COLLECTION_SIZE) {
                sanitized.put("_truncated", true);
                break;
            }
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            sanitized.put(key, isSensitiveKey(key) ? MASK : sanitizeValue(value));
            count++;
        }
        return sanitized;
    }

    private Map<String, Object> sanitizeQueryString(String query) {
        if (!StringUtils.hasText(query)) {
            return Map.of();
        }
        Map<String, List<String>> raw = new LinkedHashMap<>();
        int count = 0;
        for (String pair : query.split("&")) {
            if (count >= MAX_COLLECTION_SIZE) {
                raw.put("_truncated", List.of("true"));
                break;
            }
            String[] parts = pair.split("=", 2);
            String key = decodeQueryPart(parts[0]);
            String value = parts.length > 1 ? decodeQueryPart(parts[1]) : "";
            raw.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            count++;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (isSensitiveKey(key)) {
                sanitized.put(key, MASK);
                continue;
            }
            if (values == null || values.isEmpty()) {
                sanitized.put(key, "");
            } else if (values.size() == 1) {
                sanitized.put(key, sanitizeValue(values.get(0)));
            } else {
                sanitized.put(key, sanitizeCollection(values));
            }
        }
        return sanitized;
    }

    private String decodeQueryPart(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private List<Object> sanitizeCollection(Collection<?> collection) {
        List<Object> sanitized = new ArrayList<>();
        int count = 0;
        for (Object item : collection) {
            if (count >= MAX_COLLECTION_SIZE) {
                sanitized.add("_truncated");
                break;
            }
            sanitized.add(sanitizeValue(item));
            count++;
        }
        return sanitized;
    }

    private List<Object> sanitizeArray(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        List<Object> sanitized = new ArrayList<>();
        for (int i = 0; i < Math.min(length, MAX_COLLECTION_SIZE); i++) {
            sanitized.add(sanitizeValue(java.lang.reflect.Array.get(array, i)));
        }
        if (length > MAX_COLLECTION_SIZE) {
            sanitized.add("_truncated");
        }
        return sanitized;
    }

    private boolean isSimpleType(Object value) {
        return value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof java.time.temporal.Temporal
            || value.getClass().isPrimitive();
    }

    private boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password")
            || lower.contains("pwd")
            || lower.contains("token")
            || lower.contains("authorization")
            || lower.contains("secret")
            || lower.contains("key")
            || lower.contains("salt")
            || lower.contains("captcha")
            || lower.contains("code")
            || lower.contains("idcard")
            || lower.contains("id_card")
            || lower.contains("phone")
            || lower.contains("email");
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH) + "...";
    }
}
