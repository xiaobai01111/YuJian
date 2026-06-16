package com.campus.wall.config;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.R;
import com.campus.wall.common.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException_forbidden_returns403() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(
            new BusinessException(ResultCode.FORBIDDEN, "无权访问"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void handleBusinessException_permissionDenied_returns403() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(
            new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.PERMISSION_DENIED.getCode());
    }

    @Test
    void handleBusinessException_rateLimit_returns429() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(
            new BusinessException(ResultCode.TOO_MANY_REQUESTS, "请求过于频繁"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.TOO_MANY_REQUESTS.getCode());
    }

    @Test
    void handleBusinessException_tokenExpired_returns401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(
            new BusinessException(ResultCode.TOKEN_EXPIRED, "登录已过期"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.TOKEN_EXPIRED.getCode());
    }

    @Test
    void handleBusinessException_otherBusinessCode_keeps200() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        ResponseEntity<R<Void>> response = handler.handleBusinessException(
            new BusinessException(ResultCode.LOGIN_FAILED, "用户名或密码错误"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.LOGIN_FAILED.getCode());
    }
}
