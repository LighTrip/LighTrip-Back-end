package com.kauniv.lightrip.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 토스페이먼츠 {@code POST /v1/payments/confirm} 응답 파싱용 DTO.
 * <p>토스 응답에는 필드가 매우 많아서 우리가 쓰는 값만 매핑하고 나머지는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(

        String paymentKey,
        String orderId,
        String orderName,
        String status,       // DONE(승인 완료), CANCELED, ABORTED ...
        Long totalAmount,
        String method,       // 카드, 가상계좌, 간편결제 ...
        String approvedAt    // 승인 시각 (ISO-8601)
) {
}
