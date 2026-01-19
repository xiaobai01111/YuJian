package com.campus.wall.service.security;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Simple Redis-based rate limiter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void checkRateLimit(String key, int limit, int windowSeconds, ResultCode code) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > limit) {
                throw new BusinessException(code);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rate limit check failed for key {}: {}", key, e.getMessage());
        }
    }
}
