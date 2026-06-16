package com.campus.wall.controller.auth;

import com.campus.wall.config.SecurityProperties;
import com.campus.wall.dto.auth.LoginDTO;
import com.campus.wall.service.auth.AuthService;
import com.campus.wall.vo.auth.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private SecurityProperties securityProperties;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        authController = new AuthController(authService, securityProperties);
    }

    @Test
    void login_forceSecureEnabled_setsSecureCookie() {
        securityProperties.setRefreshCookieForceSecure(true);
        when(authService.login(any())).thenReturn(buildLoginVO());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(buildLoginDTO(), request, response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("Secure");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Strict");
    }

    @Test
    void login_forceSecureDisabled_fallsBackToForwardedProto() {
        securityProperties.setRefreshCookieForceSecure(false);
        when(authService.login(any())).thenReturn(buildLoginVO());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(buildLoginDTO(), request, response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("Secure");
    }

    @Test
    void login_forceSecureDisabled_andPlainHttp_doesNotSetSecureCookie() {
        securityProperties.setRefreshCookieForceSecure(false);
        when(authService.login(any())).thenReturn(buildLoginVO());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.login(buildLoginDTO(), request, response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).doesNotContain("Secure");
    }

    private LoginDTO buildLoginDTO() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Admin@123");
        return dto;
    }

    private LoginVO buildLoginVO() {
        LoginVO vo = new LoginVO();
        vo.setToken("access-token");
        vo.setRefreshToken("refresh-token-1234567890");
        vo.setRefreshTokenExpiresIn(3600L);
        return vo;
    }
}
