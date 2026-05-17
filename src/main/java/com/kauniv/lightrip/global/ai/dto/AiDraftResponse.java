package com.kauniv.lightrip.global.ai.dto;

import com.kauniv.lightrip.global.enums.Category;

public record AiDraftResponse(

        String draft,
        // > AI가 생성한 블로그 초안. 프론트에서 content 입력창에 미리 채워줄 값.

        Category category
        // > AI가 분류한 카테고리. 프론트에서 카테고리 선택창에 미리 선택해줄 값.
) {}