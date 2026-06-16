package com.campus.wall.integration;

import com.campus.wall.dto.auth.LoginDTO;
import com.campus.wall.dto.auth.RegisterDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.support.IntegrationTestBase;
import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @BeforeEach
    void resetLoginRateLimitKeys() {
        cleanLoginRateLimitKeys();
    }

    @Test
    void loginSuccessAndInfo() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Admin@123");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        String token = login.path("data").path("token").asText(null);
        assertNotNull(token);

        mockMvc.perform(get("/api/v1/auth/info")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void loginWrongPassword() throws Exception {
        User user = createUser(createUsername(), "Password@1", createDept(0, 3).getId(), 0);
        bindRole(user.getId(), 3L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("bad");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginUserNotFound() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("missing_" + UUID.randomUUID());
        dto.setPassword("any");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginBannedUser() throws Exception {
        User user = createUser(createUsername(), "Password@1", createDept(0, 3).getId(), 1);
        bindRole(user.getId(), 3L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Password@1");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginMissingRole() throws Exception {
        User user = createUser(createUsername(), "Password@1", createDept(0, 3).getId(), 0);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Password@1");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginMissingDept() throws Exception {
        User user = createUser(createUsername(), "Password@1", null, 0);
        bindRole(user.getId(), 3L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Password@1");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginDeptDisabled() throws Exception {
        SysDept dept = createDept(1, 3);
        User user = createUser(createUsername(), "Password@1", dept.getId(), 0);
        bindRole(user.getId(), 3L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Password@1");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void loginDeptInvalidDataScope() throws Exception {
        SysDept dept = createDept(0, 6);
        User user = createUser(createUsername(), "Password@1", dept.getId(), 0);
        bindRole(user.getId(), 3L);

        LoginDTO dto = new LoginDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword("Password@1");

        JsonNode login = postJson("/api/v1/auth/login", dto);
        assertThat(login.path("code").asInt(), not(200));
    }

    @Test
    void registerPasswordMismatch() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(createUsername());
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@2");
        dto.setEmail("test_" + UUID.randomUUID().toString().substring(0, 8) + "@edu.cn");

        JsonNode result = postJson("/api/v1/auth/register", dto);
        assertThat(result.path("code").asInt(), not(200));
    }

    @Test
    void registerDuplicateUsername() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("admin");
        dto.setPassword("Password@1");
        dto.setConfirmPassword("Password@1");
        dto.setEmail("duplicate_" + UUID.randomUUID().toString().substring(0, 8) + "@edu.cn");

        JsonNode result = postJson("/api/v1/auth/register", dto);
        assertThat(result.path("code").asInt(), not(200));
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createUsername() {
        return "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private SysDept createDept(Integer status, Integer dataScope) {
        SysDept dept = new SysDept();
        dept.setParentId(1L);
        dept.setDeptName("dept_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setSortOrder(0);
        dept.setStatus(status);
        dept.setDataScope(dataScope);
        deptMapper.insert(dept);
        return dept;
    }

    private User createUser(String username, String rawPassword, Long deptId, Integer status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(rawPassword));
        user.setNickname(username);
        user.setDeptId(deptId);
        user.setStatus(status);
        user.setUserType(0);
        user.setVerifyStatus(0);
        user.setCreditScore(100);
        userMapper.insert(user);
        return user;
    }

    private void bindRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    private void cleanLoginRateLimitKeys() {
        var keys = redisTemplate.keys("rate:login:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
