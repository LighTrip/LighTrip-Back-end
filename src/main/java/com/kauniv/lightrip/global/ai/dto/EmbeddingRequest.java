package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingRequest(

        @JsonProperty("model")
        String model,

        @JsonProperty("input")
        List<String> input,
        // > 배치 입력. 이 프로젝트는 항상 1건만 보낸다.

        @JsonProperty("dimensions")
        int dimensions
        // > passport.embedding 컬럼이 vector(1536)이므로 반드시 1536으로 고정.
        // > 명시하지 않으면 모델 기본 차원이 바뀔 때 pgvector INSERT가 깨진다.
) {

    public static EmbeddingRequest of(String model, String text, int dimensions) {
        return new EmbeddingRequest(model, List.of(text), dimensions);
    }
}
