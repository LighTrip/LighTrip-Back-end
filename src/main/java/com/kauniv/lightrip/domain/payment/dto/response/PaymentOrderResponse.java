package com.kauniv.lightrip.domain.payment.dto.response;

import com.kauniv.lightrip.domain.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 주문 생성 응답 — 프론트가 토스 결제창 호출 시 사용")
public record PaymentOrderResponse(

        @Schema(description = "주문 ID (UUID). 토스 결제창 호출 + confirm 시 동일하게 사용", example = "8c7f3a1e-9b4d-4f2c-a0e3-...")
        String orderId,

        @Schema(description = "주문을 생성한 사용자 ID", example = "1")
        Long userId,

        @Schema(description = "결제 금액 (KRW 원 단위)", example = "4900")
        Long amount,

        @Schema(description = "주문명 (사용자에게 노출되는 상품명)", example = "프리미엄 1개월")
        String orderName
) {
    public static PaymentOrderResponse from(Payment p) {
        return new PaymentOrderResponse(
                p.getOrderId(),
                p.getUser().getId(),
                p.getAmount(),
                p.getProductType().getDisplayName()
        );
    }
}
