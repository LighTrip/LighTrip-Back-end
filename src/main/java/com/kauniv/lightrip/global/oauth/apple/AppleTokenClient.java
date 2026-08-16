package com.kauniv.lightrip.global.oauth.apple;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

// > Apple의 OAuth 토큰 엔드포인트(/auth/token, /auth/revoke) 호출 전담.
// > RestClientConfig의 Bean은 자체 AI 서버(ai.lightrip.cloud) 전용이라 재사용하지 않고 별도 RestClient 사용.
@Component
@RequiredArgsConstructor
public class AppleTokenClient {

    private static final String APPLE_TOKEN_URI = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URI = "https://appleid.apple.com/auth/revoke";

    private final AppleClientSecretGenerator clientSecretGenerator;
    private final RestClient restClient = RestClient.create();

    @Value("${apple.bundle-id}")
    private String bundleId;

    // > 최초 로그인 시 RN이 넘겨준 authorizationCode를 Apple의 refresh_token으로 교환.
    // > 이 refresh_token을 Auth.refreshToken에 저장해뒀다가 탈퇴 시 revoke()에 사용함.
    public String exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", bundleId);
        form.add("client_secret", clientSecretGenerator.generate());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");

        Map<String, Object> response = restClient.post()
                .uri(APPLE_TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        return response != null ? (String) response.get("refresh_token") : null;
    }

    // > 회원탈퇴 시 저장해둔 refresh_token으로 Apple 쪽 연동을 해제 (App Store 심사 가이드라인 5.1.1(v)).
    public void revoke(String appleRefreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", bundleId);
        form.add("client_secret", clientSecretGenerator.generate());
        form.add("token", appleRefreshToken);
        form.add("token_type_hint", "refresh_token");

        restClient.post()
                .uri(APPLE_REVOKE_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
