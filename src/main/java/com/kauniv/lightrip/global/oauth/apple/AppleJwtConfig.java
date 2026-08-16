package com.kauniv.lightrip.global.oauth.apple;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

// > RN 앱이 iOS 네이티브 Sign in with Apple SDK로 직접 받아온 identityToken을 백엔드로 넘기는 구조라
// > Spring Security의 oauth2Login(인가코드 리다이렉트 흐름)을 안 타고, 이 JwtDecoder로 토큰만 검증함.
// > NimbusJwtDecoder가 Apple의 공개키(JWKS)를 가져와 캐싱하고 kid로 매칭해 서명을 검증함 — 별도 라이브러리 추가 불필요
// > (spring-boot-starter-security-oauth2-resource-server에 이미 포함된 nimbus-jose-jwt 사용).
@Configuration
public class AppleJwtConfig {

    private static final String APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.bundle-id}")
    private String appleBundleId;

    @Bean
    public JwtDecoder appleJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWK_SET_URI).build();

        // > iss(발급자)+exp/nbf 기본 검증 + aud(Bundle ID) 커스텀 검증을 합쳐서 하나의 validator로 구성
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(APPLE_ISSUER);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(appleBundleId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Apple identityToken의 aud가 Bundle ID와 일치하지 않습니다.", null));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));
        return decoder;
    }
}
