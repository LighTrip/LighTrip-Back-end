package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbeddingResponse(

        @JsonProperty("embedding")
        float[] embedding
        // > FastAPI /get-embedding이 반환하는 1536차원 float 벡터.
        // > pgvector 저장 및 유사도 검색에 사용.
) {}
