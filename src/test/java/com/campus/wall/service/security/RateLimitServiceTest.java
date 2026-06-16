package com.campus.wall.service.security;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SecurityProperties securityProperties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        rateLimitService = new RateLimitService(redisTemplate, securityProperties);
    }

    @Test
    void securityProperties_defaultRateLimitFailClosedEnabled() {
        assertThat(new SecurityProperties().isRateLimitFailClosed()).isTrue();
    }

    @Test
    void checkRateLimit_whenRedisUnavailableAndFailClosed_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rl:test")).thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> rateLimitService.checkRateLimit(
            "rl:test", 5, 60, ResultCode.RATE_LIMIT_EXCEEDED))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.RATE_LIMIT_EXCEEDED.getCode());
    }

    @Test
    void checkRateLimit_whenRedisUnavailableAndFailOpenConfigured_allows() {
        securityProperties.setRateLimitFailClosed(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rl:test")).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> rateLimitService.checkRateLimit(
            "rl:test", 5, 60, ResultCode.RATE_LIMIT_EXCEEDED)).doesNotThrowAnyException();
    }
}
