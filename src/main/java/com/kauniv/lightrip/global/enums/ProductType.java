package com.kauniv.lightrip.global.enums;

import lombok.Getter;

@Getter
public enum ProductType {
    PREMIUM_1MONTH(4900L, "프리미엄 1개월", 1),
    PREMIUM_1YEAR(49000L, "프리미엄 1년", 12);

    private final Long amount;
    private final String displayName;
    private final int months; // 이용권 제공 기간(개월) — 만료일 계산에 사용

    ProductType(Long amount, String displayName, int months) {
        this.amount = amount;
        this.displayName = displayName;
        this.months = months;
    }
}
