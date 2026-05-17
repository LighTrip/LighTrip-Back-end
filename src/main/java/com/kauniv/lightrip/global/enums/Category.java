package com.kauniv.lightrip.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {

    CAFE("카페"),
    RESTAURANT("식당"),
    BAR("술집"),
    CULTURE("문화"),
    ACTIVITY("운동"),
    SHOPPING("쇼핑"),
    NATURE("공원"),
    ETC("기타");

    private final String displayName;
}