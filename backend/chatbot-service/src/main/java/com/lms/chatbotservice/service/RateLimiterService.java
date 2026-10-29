package com.lms.chatbotservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RateLimiterService {

    StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String userId, int maxRequests, int windowSeconds) {
        if (userId == null || userId.isBlank()) {
            userId = "guest";
        }
        String key = "rate_limit:chat:" + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count != null && count <= maxRequests;
        } catch (Exception e) {
            log.warn("Redis rate limiter unavailable: {}. Falling back to allow.", e.getMessage());
            return true;
        }
    }
}
