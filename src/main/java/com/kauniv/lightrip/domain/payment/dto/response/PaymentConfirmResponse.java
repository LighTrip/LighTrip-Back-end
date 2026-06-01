package com.kauniv.lightrip.domain.payment.dto.response;

import com.kauniv.lightrip.domain.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 승인 응답")
public record PaymentConfirmResponse(

        @Schema(description = "주문 ID", example = "8c7f3a1e-9b4d-4f2c-a0e3-...")
        String orderId,

        @Schema(description = "결제 상태 (COMPLETED = 승인 완료)", example = "COMPLETED")
        String status,

        @Schema(description = "결제 금액 (KRW 원 단위)", example = "9900")
        Long amount,

        @Schema(description = "결제 수단 (카드, 가상계좌 등)", example = "카드")
        String method,

        @Schema(description = "승인 시각 (ISO-8601)", example = "2026-06-01T12:34:56+09:00")
        String approvedAt
) {
    public static PaymentConfirmResponse of(Payment payment, TossConfirmResponse toss) {
        return new PaymentConfirmResponse(
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getAmount(),
                toss.method(),
                toss.approvedAt()
        );
    }
}
