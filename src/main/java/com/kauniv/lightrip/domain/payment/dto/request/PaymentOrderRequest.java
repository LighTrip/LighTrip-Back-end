package com.kauniv.lightrip.domain.payment.dto.request;

import com.kauniv.lightrip.global.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 주문 생성 요청")
public record PaymentOrderRequest(

        @Schema(
                description = """
                        [필수] 상품 타입 — 아래 2가지 중 택 1 (월 / 연)
                        - `PREMIUM_1MONTH` : 프리미엄 1개월 (4,900원)
                        - `PREMIUM_1YEAR`  : 프리미엄 1년(12개월) (49,000원)
                        """,
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "PREMIUM_1MONTH"
        )
        @NotNull(message = "상품 타입은 필수입니다.")
        ProductType productType
) {
}
