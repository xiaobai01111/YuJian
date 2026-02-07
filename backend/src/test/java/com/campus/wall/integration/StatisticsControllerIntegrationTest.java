package com.campus.wall.integration;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.support.IntegrationTestBase;
import com.campus.wall.support.SaTokenTestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboard_requiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/console/statistics/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_returnsStats() throws Exception {
        String token = issueAdminToken();

        MvcResult result = mockMvc.perform(get("/api/v1/console/statistics/dashboard")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(200);
        JsonNode data = body.path("data");
        assertThat(data.isObject()).isTrue();
        assertThat(data.hasNonNull("totalUsers")).isTrue();
        assertThat(data.hasNonNull("totalPosts")).isTrue();
        assertThat(data.hasNonNull("noticeTotal")).isTrue();
    }

    @Test
    void recentActivities_returnsList() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/recent-activities");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
    }

    @Test
    void recentNotices_returnsList() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/recent-notices");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
    }

    @Test
    void recentReports_returnsList() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/recent-reports");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
    }

    @Test
    void recentVerifications_returnsList() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/recent-verifications");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
    }

    @Test
    void recentOperLogs_returnsList() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/recent-oper-logs");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
    }

    @Test
    void loginLogTrend_returnsSevenDays() throws Exception {
        JsonNode body = getWithAdminToken("/api/v1/console/statistics/login-log-trend");
        assertThat(body.path("code").asInt()).isEqualTo(200);
        assertThat(body.path("data").isArray()).isTrue();
        assertThat(body.path("data").size()).isEqualTo(7);
    }

    private JsonNode getWithAdminToken(String path) throws Exception {
        String token = issueAdminToken();
        MvcResult result = mockMvc.perform(get(path)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String issueAdminToken() {
        SaTokenTestContext.bind();
        try {
            StpUtil.login(1L);
            return StpUtil.getTokenValue();
        } finally {
            SaTokenTestContext.clear();
        }
    }
}
