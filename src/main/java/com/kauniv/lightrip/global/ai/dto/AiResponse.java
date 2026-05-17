package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiResponse(

        @JsonProperty("draft")
        String draft,
        // > FastAPI가 생성한 블로그 초안 텍스트.

        @JsonProperty("category")
        String category
        // > FastAPI가 분류한 카테고리 영문 키값 (CAFE, RESTAURANT 등).
) {}