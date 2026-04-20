package com.chatapp.chatservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String STATUS_KEY = "user:status:";

    public void markOnline(String username) {
        // Set status with an expiry (e.g., 5 mins) as a heartbeat
        redisTemplate.opsForValue().set(STATUS_KEY + username, "ONLINE", Duration.ofMinutes(5));
    }

    public void markOffline(String username) {
        redisTemplate.delete(STATUS_KEY + username);
    }

    public boolean isOnline(String username) {
        return redisTemplate.hasKey(STATUS_KEY + username);
    }
}