package com.kauniv.lightrip.domain.payment.repository;

import com.kauniv.lightrip.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    // > 이용권 만료일 계산용 — 완료된 결제를 승인 시각(updatedAt) 오름차순으로.
    //   누적(스탁) 계산을 위해 시간순 정렬이 필요함.
    List<Payment> findByUser_IdAndStatusOrderByUpdatedAtAsc(Long userId, Payment.PaymentStatus status);
}
