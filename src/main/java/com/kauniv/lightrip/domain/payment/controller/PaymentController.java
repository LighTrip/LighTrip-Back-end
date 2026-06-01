package com.kauniv.lightrip.domain.payment.controller;

import com.kauniv.lightrip.domain.payment.dto.request.PaymentConfirmRequest;
import com.kauniv.lightrip.domain.payment.dto.request.PaymentOrderRequest;
import com.kauniv.lightrip.domain.payment.dto.response.PaymentConfirmResponse;
import com.kauniv.lightrip.domain.payment.dto.response.PaymentOrderResponse;
import com.kauniv.lightrip.domain.payment.dto.response.PremiumStatusResponse;
import com.kauniv.lightrip.domain.payment.service.PaymentService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "토스페이먼츠 결제 API")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 주문 생성",
            description = """
                    프론트가 토스 결제창을 호출하기 직전에 호출합니다.

                    ### 선택 가능 상품 (productType — 월 / 연 중 택 1)
                    - `PREMIUM_1MONTH` : 프리미엄 1개월 (4,900원)
                    - `PREMIUM_1YEAR`  : 프리미엄 1년(12개월) (49,000원)

                    ### 동작
                    1. `productType`을 받아 백엔드의 가격 정책(ProductType enum)에서 `amount`를 결정
                    2. `orderId`(UUID) 생성, `status=PENDING`으로 DB에 저장
                    3. 프론트는 응답의 `orderId`, `amount`, `orderName`을 토스 결제창 SDK에 그대로 전달

                    ### 응답
                    - `orderId`, `userId`, `amount`, `orderName` 반환
                    - `userId` : 주문을 생성한 사용자 ID (토큰에서 추출됨)

                    ### 보안
                    - 프론트는 가격을 모름 — 위변조 불가
                    - 최종 confirm 시 DB의 `amount`와 토스 응답 `amount`가 일치하는지 한 번 더 검증
                    """)
    @PostMapping("/orders")
    public ApiResponse<PaymentOrderResponse> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentOrderRequest request
    ) {
        return ApiResponse.success("주문이 생성되었습니다.", paymentService.createOrder(userId, request));
    }

    @Operation(summary = "결제 승인",
            description = """
                    토스 결제창에서 결제가 완료된 뒤, 프론트가 받은 값으로 최종 승인을 요청합니다.
                    이 API를 호출해야 실제 결제가 확정됩니다(미호출 시 매입 취소).

                    ### 동작
                    1. `orderId`로 주문을 조회하고 소유자/상태(PENDING)를 검증
                    2. DB의 `amount`와 요청 `amount`가 일치하는지 검증(위변조 차단)
                    3. 토스 `POST /v1/payments/confirm` 호출로 최종 승인
                    4. 성공 시 `status=COMPLETED`, `paymentKey` 저장 / 실패 시 `status=FAILED`

                    ### 요청 값
                    - `paymentKey`, `orderId`, `amount` : 토스 결제창 성공 콜백으로 전달받은 값 그대로
                    """)
    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ApiResponse.success("결제가 완료되었습니다.", paymentService.confirmPayment(userId, request));
    }

    @Operation(summary = "내 프리미엄 이용권 상태 조회",
            description = """
                    로그인한 사용자의 현재 프리미엄 이용권 상태를 반환합니다.

                    ### 동작
                    - 별도 구독 테이블 없이, 완료(COMPLETED)된 결제 내역만으로 만료일을 즉석 계산
                    - 여러 번 구매 시 기간이 누적(스탁)됨 — 이용권이 남은 상태에서 추가 구매하면 끝에 이어붙임
                    - `premium` : 현재 이용 중 여부 / `expiresAt` : 만료 시각 / `daysLeft` : 남은 일수

                    ### 참고
                    - 결제 이력이 없으면 `premium=false`, `expiresAt=null`
                    - 이미 만료된 경우 `premium=false`이며 `expiresAt`은 과거 시각일 수 있음
                    """)
    @GetMapping("/me/premium")
    public ApiResponse<PremiumStatusResponse> getPremiumStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(paymentService.getPremiumStatus(userId));
    }
}
