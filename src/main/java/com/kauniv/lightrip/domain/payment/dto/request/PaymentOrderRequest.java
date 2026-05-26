package com.kauniv.lightrip.domain.payment.dto.request;

import com.kauniv.lightrip.global.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 주문 생성 요청")
public record PaymentOrderRequest(

        @Schema(
                description = "[필수] 상품 타입 (PREMIUM_1MONTH, PREMIUM_3MONTH, PREMIUM_1YEAR)",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "PREMIUM_1MONTH"
        )
        @NotNull(message = "상품 타입은 필수입니다.")
        ProductType productType
) {
}
