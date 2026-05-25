package com.kauniv.lightrip.global.auth.controller;

import com.kauniv.lightrip.global.auth.dto.TokenResponse;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.redis.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// > dev 프로파일에서만 활성화되는 테스트용 토큰 발급 컨트롤러.
// > @Profile("dev") 로 prod 배포 시 Bean 자체가 등록되지 않아 엔드포인트가 노출되지 않음.
// > k6 부하 테스트 시 VU(가상 유저)별로 다른 userId 토큰을 setup()에서 발급받기 위해 사용.
// > JwtProvider, RedisService를 그대로 재사용하므로 실제 로그인과 동일한 토큰 구조.
@Profile("dev")
@Tag(name = "Dev", description = "[DEV 전용] 테스트 토큰 발급 API")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevAuthController {

    // > JwtProvider: userId를 subject로 서명한 accessToken/refreshToken 생성
    private final JwtProvider jwtProvider;

    // > RedisService: 발급한 refreshToken을 Redis에 저장 (refresh:{userId} → hash(token))
    // > 실제 로그인(OAuth2SuccessHandler)과 동일한 방식으로 저장해야 /auth/refresh 재사용 가능
    private final RedisService redisService;

    @Operation(
            summary = "[DEV 전용] 테스트 토큰 발급",
            description = "userId로 accessToken과 refreshToken을 즉시 발급합니다. dev 환경 전용이며 prod에서는 동작하지 않습니다."
    )
    @GetMapping("/token/{userId}")
    public ResponseEntity<TokenResponse> getTestToken(@PathVariable Long userId) {
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // > refreshToken을 Redis에 저장해야 /auth/refresh 호출 시 검증 통과
        redisService.saveRefreshToken(userId, refreshToken, jwtProvider.getRefreshTokenExpiration());

        return ResponseEntity.ok(
                TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build()
        );
    }
}
