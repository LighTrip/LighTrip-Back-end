package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

// > OpenAI /v1/embeddings 응답: {"data":[{"index":0,"embedding":[...]}], "model":..., "usage":{...}}
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingResponse(

        @JsonProperty("data")
        List<Item> data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("embedding")
            List<Float> embedding
    ) {}

    // > 첫 번째 임베딩을 float[]로 변환. 없으면 빈 배열.
    // > pgvector CAST("..." AS vector) 형식 변환에 사용.
    public float[] firstEmbedding() {
        if (data == null || data.isEmpty()) return new float[0];
        Item first = data.getFirst();
        if (first == null || first.embedding() == null || first.embedding().isEmpty()) return new float[0];
        List<Float> values = first.embedding();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    // > record의 중첩 List 필드는 기본 equals가 참조 비교 → 내용 기반 비교로 오버라이드.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingResponse other)) return false;
        return Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    // > 벡터 1536개를 그대로 찍으면 로그가 터진다 — 길이만 남긴다.
    @Override
    public String toString() {
        return "EmbeddingResponse{vectorLen=" + firstEmbedding().length + "}";
    }
}
