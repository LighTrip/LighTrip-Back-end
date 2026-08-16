package com.kauniv.lightrip.global.auth.service;

import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.auth.dto.TokenResponse;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;

    // > refreshToken을 회전시키므로 새 refreshToken도 함께 반환해야 한다.
    // > 반환하지 않으면 클라이언트가 이전 토큰을 계속 들고 있어 다음 재발급이 REFRESH_TOKEN_MISMATCH로 실패한다.
    public TokenResponse reissueAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!redisService.validateRefreshToken(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);
        redisService.saveRefreshToken(userId, newRefreshToken, jwtProvider.getRefreshTokenExpiration());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(Long userId, String accessToken) {
        long expiration = jwtProvider.getExpiration(accessToken);
        if (expiration > 0) {
            redisService.addBlacklist(accessToken, expiration);
        }
        redisService.deleteRefreshToken(userId);
    }

    public void withdraw(Long userId, String accessToken) {
        userRepository.deleteById(userId); // DB CASCADE가 auth 포함 연관 데이터 전부 삭제
        long expiration = jwtProvider.getExpiration(accessToken);
        if (expiration > 0) {
            redisService.addBlacklist(accessToken, expiration);
        }
        redisService.deleteRefreshToken(userId);
    }
}