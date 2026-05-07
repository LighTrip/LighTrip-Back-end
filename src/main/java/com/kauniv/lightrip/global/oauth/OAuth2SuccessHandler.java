package com.kauniv.lightrip.global.oauth;

import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.auth.repository.AuthRepository;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.redis.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;
    private final RedisService redisService;

    @Value("${app.deep-link.scheme}")
    private String deepLinkScheme;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getUserId();
        SocialType socialType = oAuth2User.getSocialType();

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        Auth auth = authRepository.findByUser_IdAndSocialType(userId, socialType)
                .orElseThrow(() -> new RuntimeException("Auth를 찾을 수 없습니다."));

        redisService.saveRefreshToken(userId, refreshToken, jwtProvider.getRefreshTokenExpiration());

        log.debug("ACCESS TOKEN: {}", accessToken);
        log.debug("REFRESH TOKEN: {}", refreshToken);

        boolean isNewUser = oAuth2User.isNewUser();

        String deepLink = String.format(
                "%s://auth/callback?accessToken=%s&refreshToken=%s&isNewUser=%s",
                deepLinkScheme, accessToken, refreshToken, isNewUser
        );

        getRedirectStrategy().sendRedirect(request, response, deepLink);
    }
}