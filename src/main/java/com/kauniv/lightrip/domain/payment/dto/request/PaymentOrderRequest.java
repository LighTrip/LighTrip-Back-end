package com.kauniv.lightrip.domain.payment.dto.request;

import com.kauniv.lightrip.global.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 주문 생성 요청")
public record PaymentOrderRequest(

        @Schema(
                description = """
                        [필수] 상품 타입 — 아래 3가지 중 택 1 (1개월 / 3개월 / 12개월)
                        - `PREMIUM_1MONTH` : 프리미엄 1개월 (9,900원)
                        - `PREMIUM_3MONTH` : 프리미엄 3개월 (27,000원)
                        - `PREMIUM_1YEAR`  : 프리미엄 12개월(1년) (99,000원)
                        """,
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "PREMIUM_1MONTH"
        )
        @NotNull(message = "상품 타입은 필수입니다.")
        ProductType productType
) {
}
