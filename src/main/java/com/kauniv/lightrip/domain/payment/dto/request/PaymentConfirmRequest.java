package com.kauniv.lightrip.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 승인 요청 — 토스 결제창 성공 후 프론트가 전달하는 값")
public record PaymentConfirmRequest(

        @Schema(
                description = "[필수] 토스가 발급한 결제 키 (결제창 성공 콜백으로 전달됨)",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "tviva20240101..."
        )
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @Schema(
                description = "[필수] 주문 ID (주문 생성 시 발급받은 orderId와 동일)",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "8c7f3a1e-9b4d-4f2c-a0e3-..."
        )
        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        @Schema(
                description = "[필수] 결제 금액 (KRW 원 단위). DB에 저장된 금액과 일치해야 승인됨 — 위변조 검증용",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "9900"
        )
        @NotNull(message = "amount는 필수입니다.")
        Long amount
) {
}
