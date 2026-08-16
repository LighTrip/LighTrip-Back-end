package com.kauniv.lightrip.global.oauth.apple;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

// > Apple 토큰 교환(/auth/token)·해제(/auth/revoke) API를 호출할 때 우리 서버 신원을 증명하는
// > client_secret(ES256 서명 JWT)을 그때그때 생성함. 로그인 검증(identityToken)과는 반대 방향 —
// > 여기선 우리가 Apple Developer에서 받은 개인키(.p8)로 직접 서명함.
// > 호출마다 새로 만들고 만료를 짧게(5분) 잡음. 캐싱해서 재사용해도 되지만(Apple 최대 6개월 허용),
// > 호출 빈도가 낮아(회원가입/탈퇴 시점뿐) 캐싱 이점이 크지 않아 단순하게 매번 생성.
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.bundle-id}")
    private String bundleId;

    @Value("${apple.private-key}")
    private String privateKeyValue;

    public String generate() {
        Instant now = Instant.now();

        return Jwts.builder()
                .header().add("kid", keyId).and()
                .issuer(teamId)
                .subject(bundleId)
                .audience().add(APPLE_AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    // > .env엔 개행이 없는 한 줄로 저장하는 게 편해서(예: "-----BEGIN PRIVATE KEY-----\nMII...\n-----END..."),
    // > 헤더/푸터 문자열과 실제 개행, 이스케이프된 "\n" 리터럴을 전부 제거하고 순수 base64만 남겨서 디코딩.
    private PrivateKey loadPrivateKey() {
        String base64 = privateKeyValue
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");

        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("apple.private-key 파싱 실패 — .p8 키 형식을 확인하세요.", e);
        }
    }
}
