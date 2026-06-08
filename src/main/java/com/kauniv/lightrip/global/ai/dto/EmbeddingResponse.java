package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public record EmbeddingResponse(
        @JsonProperty("embedding")
        float[] embedding
        // > FastAPI /get-embedding이 반환하는 1536차원 float 벡터.
        // > pgvector 저장 및 유사도 검색에 사용.
) {
    // > record는 배열 필드를 참조로 비교하므로 내용 기반 비교로 오버라이드.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingResponse other)) return false;
        return Arrays.equals(embedding, other.embedding);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(embedding);
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{dim=" + (embedding != null ? embedding.length : 0) + "}";
    }
}
