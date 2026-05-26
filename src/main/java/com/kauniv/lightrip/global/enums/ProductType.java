package com.kauniv.lightrip.global.enums;

import lombok.Getter;

@Getter
public enum ProductType {
    PREMIUM_1MONTH(9900L, "프리미엄 1개월"),
    PREMIUM_3MONTH(27000L, "프리미엄 3개월"),
    PREMIUM_1YEAR(99000L, "프리미엄 1년");

    private final Long amount;
    private final String displayName;

    ProductType(Long amount, String displayName) {
        this.amount = amount;
        this.displayName = displayName;
    }
}
