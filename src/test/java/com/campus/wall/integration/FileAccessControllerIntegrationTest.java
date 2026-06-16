package com.campus.wall.integration;

import com.campus.wall.entity.file.FileRecord;
import com.campus.wall.enums.file.FileAuditStatus;
import com.campus.wall.enums.file.FileTargetType;
import com.campus.wall.enums.file.FileVisibility;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
    "app.storage.local-path=/tmp/campus-wall-test-uploads",
    "app.storage.signing-secret=test-secret"
})
class FileAccessControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @BeforeEach
    void setUp() {
        fileRecordMapper.delete(null);
    }

    @Test
    void preview_missingFile_returnsNotFoundCode() throws Exception {
        String body = mockMvc.perform(get("/api/v1/files/preview/999999"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asInt()).isEqualTo(404);
    }

    @Test
    void preview_invalidFileId_returnsBadRequest() throws Exception {
        String body = mockMvc.perform(get("/api/v1/files/preview/abc"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asInt()).isEqualTo(404);
    }

    @Test
    void preview_privateMissingSignature_forbidden() throws Exception {
        FileRecord record = prepareFileRecord(FileVisibility.PRIVATE, "private.txt");

        String body = mockMvc.perform(get("/api/v1/files/preview/" + record.getPublicKey()))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asInt()).isEqualTo(403);
        assertThat(body).doesNotContain(record.getPath());
    }

    @Test
    void preview_privateInvalidSignature_forbidden() throws Exception {
        FileRecord record = prepareFileRecord(FileVisibility.PRIVATE, "private.txt");

        String body = mockMvc.perform(get("/api/v1/files/preview/" + record.getPublicKey())
                .param("expires", String.valueOf(System.currentTimeMillis() / 1000 + 300))
                .param("sig", "invalid"))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asInt()).isEqualTo(403);
        assertThat(body).doesNotContain(record.getPath());
    }

    @Test
    void preview_public_sanitizesFilename() throws Exception {
        FileRecord record = prepareFileRecord(FileVisibility.PUBLIC, "../secret.txt");

        var response = mockMvc.perform(get("/api/v1/files/preview/" + record.getPublicKey()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        String contentDisposition = response.getHeader("Content-Disposition");
        assertThat(contentDisposition).isNotNull();
        assertThat(contentDisposition).doesNotContain("..");
        assertThat(contentDisposition).doesNotContain("/");
        assertThat(contentDisposition).doesNotContain("\\");
        assertThat(response.getContentAsString()).isEqualTo("hello");
    }

    private FileRecord prepareFileRecord(FileVisibility visibility, String filename) throws Exception {
        Path base = Path.of("/tmp/campus-wall-test-uploads");
        Files.createDirectories(base);
        String path = "test/" + System.nanoTime() + ".txt";
        Path target = base.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, "hello");

        FileRecord record = new FileRecord();
        record.setUserId(1L);
        record.setFilename(filename);
        record.setPath(path);
        record.setSize(5L);
        record.setMimeType("text/plain");
        record.setStatus(0);
        record.setAuditStatus(FileAuditStatus.PASSED.getCode());
        record.setStorageClass("STANDARD");
        record.setStorageProvider(StorageProviderType.LOCAL.getCode());
        record.setVisibility(visibility.getCode());
        record.setAssetType(FileTargetType.FILE.getCode());
        record.setPublicKey(UUID.randomUUID().toString().replace("-", ""));
        record.setTargetType(FileTargetType.FILE.getCode());
        record.setCreatedAt(LocalDateTime.now());
        fileRecordMapper.insert(record);
        return record;
    }
}
