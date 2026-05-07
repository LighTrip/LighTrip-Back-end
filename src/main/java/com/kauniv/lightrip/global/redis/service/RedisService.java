package com.kauniv.lightrip.global.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 해싱 실패", e);
        }
    }

    public void addBlacklist(String accessToken, long expirationMs) {
        redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + accessToken, "logout", expirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }

    public void saveRefreshToken(Long userId, String refreshToken, long expirationMs) {
        redisTemplate.opsForValue()
                .set(REFRESH_TOKEN_PREFIX + userId, hash(refreshToken), expirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String saved = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        return saved != null && saved.equals(hash(refreshToken));
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}