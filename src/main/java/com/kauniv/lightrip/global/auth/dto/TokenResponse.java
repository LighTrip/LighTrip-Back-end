package com.kauniv.lightrip.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// > accessToken은 항상 포함, refreshToken은 dev 토큰 발급 시에만 포함
// > @JsonInclude(NON_NULL) 로 null 필드는 응답 JSON에서 제외
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}