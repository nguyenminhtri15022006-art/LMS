package com.lms.chatbotservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    RateLimiterService rateLimiterService;

    @Test
    void tryAcquireReturnsTrueWhenUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        boolean acquired = rateLimiterService.tryAcquire("user-1", 10, 60);

        assertThat(acquired).isTrue();
    }

    @Test
    void tryAcquireSetsExpirationOnFirstRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean acquired = rateLimiterService.tryAcquire("user-new", 10, 60);

        assertThat(acquired).isTrue();
        verify(redisTemplate).expire(eq("rate_limit:chat:user-new"), any(Duration.class));
    }

    @Test
    void tryAcquireReturnsFalseWhenOverLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        boolean acquired = rateLimiterService.tryAcquire("user-spam", 10, 60);

        assertThat(acquired).isFalse();
    }
}
