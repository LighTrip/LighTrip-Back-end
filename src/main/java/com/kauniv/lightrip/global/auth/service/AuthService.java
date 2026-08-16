package com.kauniv.lightrip.global.auth.service;

import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.auth.dto.TokenResponse;
import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.auth.repository.AuthRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.oauth.SocialType;
import com.kauniv.lightrip.global.oauth.apple.AppleTokenClient;
import com.kauniv.lightrip.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final AuthRepository authRepository;
    private final AppleTokenClient appleTokenClient;

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
        revokeAppleIfLinked(userId); // CASCADE로 Auth가 같이 삭제되기 전에 revoke용 refreshToken을 먼저 사용
        userRepository.deleteById(userId); // DB CASCADE가 auth 포함 연관 데이터 전부 삭제
        long expiration = jwtProvider.getExpiration(accessToken);
        if (expiration > 0) {
            redisService.addBlacklist(accessToken, expiration);
        }
        redisService.deleteRefreshToken(userId);
    }

    // > Apple 계정으로 가입한 유저가 탈퇴하면 Apple 쪽 연동도 끊어줘야 함 (App Store 심사 가이드라인 5.1.1(v)).
    // > revoke 실패해도 탈퇴 자체는 막지 않음 — Apple API 장애로 유저가 탈퇴를 못 하게 되는 게 더 나쁨.
    private void revokeAppleIfLinked(Long userId) {
        authRepository.findByUser_IdAndSocialType(userId, SocialType.APPLE)
                .map(Auth::getRefreshToken)
                .filter(token -> token != null && !token.isBlank())
                .ifPresent(token -> {
                    try {
                        appleTokenClient.revoke(token);
                    } catch (Exception e) {
                        log.warn("Apple 계정 연동 해제 실패, userId={}", userId, e);
                    }
                });
    }
}