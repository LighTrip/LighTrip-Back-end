package com.kauniv.lightrip.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Visibility {

    PUBLIC("공개"),
    PRIVATE("비공개"),
    FRIENDS_ONLY("친구만 공개");

    private final String displayName;
}