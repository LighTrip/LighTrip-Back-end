package com.kauniv.lightrip.global.auth.service;

import com.kauniv.lightrip.domain.user.service.UserService;
import com.kauniv.lightrip.global.auth.dto.AppleLoginRequest;
import com.kauniv.lightrip.global.auth.dto.LoginResponse;
import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.oauth.SocialType;
import com.kauniv.lightrip.global.oauth.apple.AppleTokenClient;
import com.kauniv.lightrip.global.oauth.userinfo.AppleOAuth2UserInfo;
import com.kauniv.lightrip.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// > 카카오는 Spring Security oauth2Login(인가코드 리다이렉트) 흐름을 타지만,
// > Apple은 RN 네이티브 SDK가 이미 발급받은 identityToken을 REST로 그대로 넘겨주는 구조라
// > CustomOAuth2UserService/OAuth2SuccessHandler 대신 이 서비스가 검증부터 토큰 발급까지 전담함.
// > 이후 User/Auth 생성 로직(findOrCreateUser)은 카카오와 동일한 UserService를 그대로 재사용.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AppleAuthService {

    private final JwtDecoder appleJwtDecoder;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final AppleTokenClient appleTokenClient;

    public LoginResponse login(AppleLoginRequest request) {
        Jwt jwt;
        try {
            jwt = appleJwtDecoder.decode(request.getIdentityToken());
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String providerId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        boolean isNewUser = !userService.existsBySocialInfo(providerId, SocialType.APPLE);

        AppleOAuth2UserInfo userInfo = new AppleOAuth2UserInfo(providerId, email, request.getNickname());
        Auth auth = userService.findOrCreateUser(userInfo, SocialType.APPLE);

        if (isNewUser) {
            storeAppleRefreshToken(auth, request.getAuthorizationCode());
        }

        Long userId = auth.getUser().getId();
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        redisService.saveRefreshToken(userId, refreshToken, jwtProvider.getRefreshTokenExpiration());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .build();
    }

    // > 탈퇴 시 필요한 revoke용 refresh_token을 미리 확보해두는 과정.
    // > 실패해도 로그인 자체는 계속 진행 — 이 단계는 나중 탈퇴 흐름을 위한 부가 작업이라 로그인을 막을 이유가 없음.
    private void storeAppleRefreshToken(Auth auth, String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            log.warn("Apple authorizationCode가 없어 refreshToken 저장을 건너뜁니다. userId={}", auth.getUser().getId());
            return;
        }
        try {
            String appleRefreshToken = appleTokenClient.exchangeAuthorizationCode(authorizationCode);
            if (appleRefreshToken != null) {
                auth.updateRefreshToken(appleRefreshToken);
            }
        } catch (Exception e) {
            log.warn("Apple authorizationCode 교환 실패, userId={}", auth.getUser().getId(), e);
        }
    }
}
