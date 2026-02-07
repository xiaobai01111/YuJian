package com.campus.wall.service.content.impl;

import com.campus.wall.config.ContentModerationProperties;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.content.ContentModerationService;
import com.campus.wall.service.content.SensitiveWordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 内容安全审核服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private final SensitiveWordService sensitiveWordService;
    private final FileRecordMapper fileRecordMapper;
    private final ContentModerationProperties moderationProperties;
    private final ObjectMapper objectMapper;

    // 审核状态：1审核通过 2审核不通过
    private static final int AUDIT_PASSED = 1;
    private static final int AUDIT_REJECTED = 2;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Override
    public boolean moderateImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }
        if (!moderationProperties.isImageAuditEnabled()) {
            log.error("图片审核未启用，按 fail-closed={} 处理", moderationProperties.isImageAuditFailClosed());
            return !moderationProperties.isImageAuditFailClosed();
        }

        String apiUrl = moderationProperties.getImageAuditApiUrl();
        if (!StringUtils.hasText(apiUrl)) {
            log.error("图片审核服务地址未配置，按 fail-closed={} 处理", moderationProperties.isImageAuditFailClosed());
            return !moderationProperties.isImageAuditFailClosed();
        }

        int timeoutMs = Math.max(1000, moderationProperties.getImageAuditTimeoutMs());
        try {
            String payload = objectMapper.writeValueAsString(Map.of("url", imageUrl));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json");

            String token = moderationProperties.getImageAuditApiToken();
            if (StringUtils.hasText(token)) {
                builder.header("Authorization", "Bearer " + token.trim());
            }

            HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("图片审核服务返回异常状态: status={}", response.statusCode());
                return !moderationProperties.isImageAuditFailClosed();
            }

            boolean passed = parseModerationResult(response.body());
            log.info("图片审核完成: passed={}, url={}", passed, imageUrl);
            return passed;
        } catch (Exception e) {
            log.error("调用图片审核服务失败", e);
            return !moderationProperties.isImageAuditFailClosed();
        }
    }

    @Override
    public boolean moderateText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // 使用敏感词服务检测
        return !sensitiveWordService.containsSensitiveWord(text);
    }

    @Override
    public boolean scanFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        if (!moderationProperties.isVirusScanEnabled()) {
            log.error("病毒扫描未启用，按 fail-closed={} 处理", moderationProperties.isVirusScanFailClosed());
            return !moderationProperties.isVirusScanFailClosed();
        }

        try (InputStream inputStream = file.getInputStream()) {
            boolean clean = scanByClamav(inputStream);
            if (!clean) {
                log.warn("文件病毒扫描未通过: filename={}", file.getOriginalFilename());
            }
            return clean;
        } catch (Exception e) {
            log.error("病毒扫描失败: filename={}", file.getOriginalFilename(), e);
            return !moderationProperties.isVirusScanFailClosed();
        }
    }

    @Override
    @Async
    public void asyncModerateImage(Long fileId, String imageUrl) {
        try {
            boolean passed = moderateImage(imageUrl);
            int auditStatus = passed ? AUDIT_PASSED : AUDIT_REJECTED;
            fileRecordMapper.updateAuditStatus(fileId, auditStatus);
            
            if (!passed) {
                log.warn("图片审核不通过，已标记: fileId={}, url={}", fileId, imageUrl);
                // TODO: 发送通知给用户
            }
        } catch (Exception e) {
            try {
                fileRecordMapper.updateAuditStatus(fileId, AUDIT_REJECTED);
            } catch (Exception ignore) {
                // ignore update failure
            }
            log.error("异步图片审核失败: fileId={}", fileId, e);
        }
    }

    private boolean parseModerationResult(String body) {
        if (!StringUtils.hasText(body)) {
            return !moderationProperties.isImageAuditFailClosed();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            Boolean direct = extractPassValue(root);
            if (direct != null) {
                return direct;
            }
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) {
                Boolean nested = extractPassValue(data);
                if (nested != null) {
                    return nested;
                }
            }
            log.error("图片审核响应无法识别: {}", abbreviate(body));
        } catch (Exception e) {
            log.error("解析图片审核响应失败", e);
        }
        return !moderationProperties.isImageAuditFailClosed();
    }

    private Boolean extractPassValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String[] booleanKeys = {"pass", "passed", "approved", "allow", "safe"};
        for (String key : booleanKeys) {
            JsonNode value = node.get(key);
            if (value != null && value.isBoolean()) {
                return value.booleanValue();
            }
        }
        JsonNode result = node.get("result");
        if (result != null) {
            if (result.isBoolean()) {
                return result.booleanValue();
            }
            if (result.isTextual()) {
                String normalized = result.asText("").trim().toLowerCase();
                if ("pass".equals(normalized) || "ok".equals(normalized)
                    || "allow".equals(normalized) || "safe".equals(normalized)
                    || "approved".equals(normalized) || "clean".equals(normalized)) {
                    return true;
                }
                if ("reject".equals(normalized) || "block".equals(normalized)
                    || "unsafe".equals(normalized) || "violation".equals(normalized)
                    || "forbid".equals(normalized)) {
                    return false;
                }
            }
        }
        return null;
    }

    private boolean scanByClamav(InputStream inputStream) throws Exception {
        String host = moderationProperties.getClamavHost();
        int port = moderationProperties.getClamavPort();
        if (!StringUtils.hasText(host) || port <= 0 || port > 65535) {
            throw new IllegalStateException("ClamAV 配置不合法");
        }

        int connectTimeoutMs = Math.max(500, moderationProperties.getClamavConnectTimeoutMs());
        int readTimeoutMs = Math.max(1000, moderationProperties.getClamavReadTimeoutMs());
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.trim(), port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            var outputStream = socket.getOutputStream();
            var responseStream = socket.getInputStream();
            outputStream.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(ByteBuffer.allocate(4).putInt(read).array());
                outputStream.write(buffer, 0, read);
            }
            outputStream.write(new byte[] {0, 0, 0, 0});
            outputStream.flush();

            String response = readLine(responseStream);
            if (!StringUtils.hasText(response)) {
                throw new IllegalStateException("ClamAV 返回空响应");
            }
            if (response.contains("OK")) {
                return true;
            }
            if (response.contains("FOUND")) {
                return false;
            }
            throw new IllegalStateException("ClamAV 返回未知结果: " + response);
        }
    }

    private String readLine(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int b;
        int maxLength = 8192;
        while ((b = inputStream.read()) != -1 && output.size() < maxLength) {
            if (b == '\n') {
                break;
            }
            output.write(b);
        }
        return output.toString(StandardCharsets.US_ASCII).trim();
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        int max = 300;
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
