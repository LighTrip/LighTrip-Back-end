package com.kauniv.lightrip.domain.payment.service;

import com.kauniv.lightrip.domain.payment.dto.request.PaymentOrderRequest;
import com.kauniv.lightrip.domain.payment.dto.response.PaymentOrderResponse;
import com.kauniv.lightrip.domain.payment.entity.Payment;
import com.kauniv.lightrip.domain.payment.repository.PaymentRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

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
}
