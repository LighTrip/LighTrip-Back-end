package com.kauniv.lightrip.domain.payment.entity;

import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.global.enums.ProductType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_id", columnNames = {"order_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId;
    // > 토스 결제창 호출 시 클라이언트가 사용하는 주문 식별자. UUID 문자열.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private Long amount;
    // > 결제 금액 (KRW 원 단위 정수). ProductType.getAmount()로 결정됨 — 백엔드가 단일 진실 공급원.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;
    // > 토스가 confirm 시 발급하는 결제 키. PENDING 상태에서는 null.

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private ProductType productType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, CANCELED
    }

    // ========== 비즈니스 메서드 ==========

    public void markCompleted(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
