package com.kauniv.lightrip.global.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// > identityToken: iOS 네이티브 SDK가 발급한 JWT, 서버에서 Apple JWKS로 서명 검증
// > nickname: Apple이 최초 로그인 1회에만 클라이언트로 내려주는 fullName. 재로그인 시엔 없으므로 nullable.
// > authorizationCode: 매 로그인 시도마다 발급되는 1회용 코드. 최초 가입 시점에 Apple refresh_token으로
// > 교환해 저장해두고, 탈퇴 시 연동 해제(revoke)에 사용함. nullable — 없으면 교환/저장을 건너뜀.
@Getter
@NoArgsConstructor
public class AppleLoginRequest {

    @NotBlank
    private String identityToken;

    private String nickname;

    private String authorizationCode;
}
