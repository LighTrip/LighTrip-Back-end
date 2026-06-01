package com.kauniv.lightrip.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "현재 프리미엄 이용권 상태")
public record PremiumStatusResponse(

        @Schema(description = "현재 프리미엄 이용 중 여부", example = "true")
        boolean premium,

        @Schema(description = "이용권 만료 시각 (완료된 결제가 없으면 null). 만료된 경우 과거 시각일 수 있음",
                example = "2026-09-01T12:34:56")
        LocalDateTime expiresAt,

        @Schema(description = "만료까지 남은 일수 (프리미엄이 아닐 경우 null)", example = "92")
        Long daysLeft
) {
    public static PremiumStatusResponse of(boolean premium, LocalDateTime expiresAt, Long daysLeft) {
        return new PremiumStatusResponse(premium, expiresAt, daysLeft);
    }
}
