package com.kauniv.lightrip.domain.payment.service;

import com.kauniv.lightrip.domain.payment.dto.request.PaymentConfirmRequest;
import com.kauniv.lightrip.domain.payment.dto.request.PaymentOrderRequest;
import com.kauniv.lightrip.domain.payment.dto.response.PaymentConfirmResponse;
import com.kauniv.lightrip.domain.payment.dto.response.PaymentOrderResponse;
import com.kauniv.lightrip.domain.payment.dto.response.PremiumStatusResponse;
import com.kauniv.lightrip.domain.payment.dto.response.TossConfirmResponse;
import com.kauniv.lightrip.domain.payment.entity.Payment;
import com.kauniv.lightrip.domain.payment.repository.PaymentRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RestClient tossPaymentsRestClient;
    // > 필드명 = 빈 이름(tossPaymentsRestClient)으로 매칭 — RestClient 빈이 2개라 이름으로 구분.

    // ========== 주문 생성 ==========
    // > 프론트가 토스 결제창 호출하기 직전에 호출. orderId + amount + orderName 반환.
    // > amount는 ProductType enum이 단일 진실 공급원 — 프론트는 가격을 모름(위변조 차단).
    @Transactional
    public PaymentOrderResponse createOrder(Long userId, PaymentOrderRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Payment payment = Payment.builder()
                .orderId(UUID.randomUUID().toString())
                .user(user)
                .amount(req.productType().getAmount())
                .status(Payment.PaymentStatus.PENDING)
                .productType(req.productType())
                .build();

        paymentRepository.save(payment);

        return PaymentOrderResponse.from(payment);
    }

    // ========== 결제 승인(confirm) ==========
    // > 프론트가 토스 결제창에서 결제를 마치면 받은 paymentKey/orderId/amount로 호출.
    // > 토스 서버에 최종 승인을 요청해야 실제 결제가 확정됨(confirm 안 하면 매입 취소됨).
    //
    // > 트랜잭션을 NOT_SUPPORTED로 둔 이유:
    //   1) 느린 외부 HTTP 호출 동안 DB 트랜잭션/커넥션을 점유하지 않기 위함
    //   2) 토스 실패 시 markFailed()를 롤백 없이 영속화하기 위함
    //      (단일 쓰기 트랜잭션이면 예외 throw 시 FAILED 기록까지 롤백됨)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest req) {
        Payment payment = paymentRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        validateConfirmable(payment, userId, req);

        TossConfirmResponse toss;
        try {
            toss = requestTossConfirm(req);
        } catch (RestClientException e) {
            // 토스가 4xx/5xx 응답 또는 통신 오류 — 결제 실패로 기록
            log.warn("토스 결제 승인 실패 orderId={}, error={}", req.orderId(), e.getMessage());
            payment.markFailed();
            paymentRepository.save(payment);
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        payment.markCompleted(toss.paymentKey());
        paymentRepository.save(payment);
        log.info("결제 승인 완료 orderId={}, paymentKey={}, amount={}",
                payment.getOrderId(), toss.paymentKey(), payment.getAmount());

        return PaymentConfirmResponse.of(payment, toss);
    }

    // > 승인 가능 여부 검증. 쓰기 전에 호출되므로 예외가 던져져도 부작용 없음.
    private void validateConfirmable(Payment payment, Long userId, PaymentConfirmRequest req) {
        if (!payment.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
        }
        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        // 위변조 차단: 프론트가 보낸 금액 == 주문 생성 시 ProductType 기준으로 저장된 금액
        if (!payment.getAmount().equals(req.amount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    // > 토스 결제 승인 API 호출. 인증 헤더는 tossPaymentsRestClient 빈에 기본 설정됨.
    private TossConfirmResponse requestTossConfirm(PaymentConfirmRequest req) {
        return tossPaymentsRestClient.post()
                .uri("/v1/payments/confirm")
                .body(Map.of(
                        "paymentKey", req.paymentKey(),
                        "orderId", req.orderId(),
                        "amount", req.amount()
                ))
                .retrieve()
                .body(TossConfirmResponse.class);
    }

    // ========== 현재 프리미엄 이용권 상태 조회 ==========
    // > 별도 구독/만료 테이블 없이 COMPLETED 결제 내역만으로 만료일을 즉석 계산.
    //   payment 테이블이 단일 진실 공급원 — 만료일이 결제와 어긋날 일이 없음.
    //
    // > 누적(스탁) 규칙: 결제를 승인 시각 오름차순으로 훑으며,
    //   이미 이용권이 남아있으면(expiry > 승인시각) 그 끝에 기간을 이어붙이고,
    //   끊겼으면(만료 후 재구매) 승인 시각부터 새로 시작한다.
    public PremiumStatusResponse getPremiumStatus(Long userId) {
        List<Payment> completed = paymentRepository
                .findByUser_IdAndStatusOrderByUpdatedAtAsc(userId, Payment.PaymentStatus.COMPLETED);

        LocalDateTime expiry = null;
        for (Payment p : completed) {
            LocalDateTime paidAt = p.getUpdatedAt(); // 승인 완료 시각
            LocalDateTime base = (expiry != null && expiry.isAfter(paidAt)) ? expiry : paidAt;
            expiry = base.plusMonths(p.getProductType().getMonths());
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zone);
        boolean premium = expiry != null && expiry.atZone(zone).isAfter(now);
        Long daysLeft = premium ? ChronoUnit.DAYS.between(now, expiry.atZone(zone)) : null;

        return PremiumStatusResponse.of(premium, expiry, daysLeft);
    }
}
