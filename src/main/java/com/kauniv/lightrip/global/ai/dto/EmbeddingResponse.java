package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public record EmbeddingResponse(
        @JsonProperty("dim")
        int dim,
        // > FastAPI /get-embedding 응답의 임베딩 차원수 (1536).
        @JsonProperty("embeddings")
        List<List<Float>> embeddings
        // > 배치 입력 대응. 단일 텍스트 요청 시 embeddings[0]이 실제 벡터.
) {
    // > texts 배열의 첫 번째 임베딩을 float[]로 변환.
    // > pgvector CAST("..." AS vector) 형식 변환에 사용.
    public float[] firstEmbedding() {
        if (embeddings == null || embeddings.isEmpty()) return null;
        List<Float> first = embeddings.get(0);
        if (first == null || first.isEmpty()) return null;
        float[] result = new float[first.size()];
        for (int i = 0; i < first.size(); i++) {
            result[i] = first.get(i);
        }
        return result;
    }

    // > record의 List<List<Float>> 필드는 기본 equals가 참조 비교 → 내용 기반 비교로 오버라이드.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingResponse other)) return false;
        return dim == other.dim && Objects.equals(embeddings, other.embeddings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dim, embeddings);
    }

    @Override
    public String toString() {
        int size = (embeddings != null && !embeddings.isEmpty() && embeddings.get(0) != null)
                ? embeddings.get(0).size() : 0;
        return "EmbeddingResponse{dim=" + dim + ", vectorLen=" + size + "}";
    }
}
