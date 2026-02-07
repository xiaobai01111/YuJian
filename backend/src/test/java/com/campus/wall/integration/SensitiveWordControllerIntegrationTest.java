package com.campus.wall.integration;

import com.campus.wall.dto.auth.LoginDTO;
import com.campus.wall.mapper.system.SensitiveWordMapper;
import com.campus.wall.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SensitiveWordControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        sensitiveWordMapper.delete(null);
        token = loginAdmin();
    }

    @Test
    void list_requiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/system/sensitive-words"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createAndQuery_returnsRecords() throws Exception {
        Map<String, Object> body = Map.of("word", "test_word", "level", 2);

        MvcResult create = mockMvc.perform(post("/api/v1/system/sensitive-words")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode createBody = objectMapper.readTree(create.getResponse().getContentAsString());
        assertThat(createBody.path("code").asInt()).isEqualTo(200);

        MvcResult query = mockMvc.perform(get("/api/v1/system/sensitive-words")
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode queryBody = objectMapper.readTree(query.getResponse().getContentAsString());
        assertThat(queryBody.path("code").asInt()).isEqualTo(200);
        assertThat(queryBody.path("data").path("records").size()).isEqualTo(1);
        assertThat(queryBody.path("data").path("records").get(0).path("word").asText())
            .isEqualTo("test_word");
    }

    @Test
    void createBatch_emptyList_returnsFail() throws Exception {
        Map<String, Object> body = Map.of("words", List.of());

        MvcResult result = mockMvc.perform(post("/api/v1/system/sensitive-words/batch")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode resp = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(resp.path("code").asInt()).isNotEqualTo(200);
        assertThat(resp.path("message").asText()).isEqualTo("词列表不能为空");
    }

    private String loginAdmin() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Admin@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }
}
