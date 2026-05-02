package com.kauniv.lightrip.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kauniv.lightrip.global.auth.dto.LoginResponse;
import com.kauniv.lightrip.global.auth.entity.Auth;
import com.kauniv.lightrip.global.auth.repository.AuthRepository;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;
    private final ObjectMapper objectMapper;

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
        auth.updateRefreshToken(refreshToken);

        response.setContentType("application/json;charset=UTF-8");

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
    }
}