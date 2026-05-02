package com.kauniv.lightrip.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {

    CAFE("카페"),
    RESTAURANT("식당"),
    BAR("술집/바"),
    TOURIST("관광지"),
    NATURE("자연/공원"),
    CULTURE("문화/예술"),
    ACTIVITY("액티비티"),
    ACCOMMODATION("숙소"),
    SHOPPING("쇼핑"),
    ETC("기타");

    private final String displayName;
}