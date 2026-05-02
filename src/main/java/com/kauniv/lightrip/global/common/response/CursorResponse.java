package com.kauniv.lightrip.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "커서 기반 페이징 응답")
public record CursorResponse<T>(

        @Schema(description = "데이터 목록")
        List<T> content,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,

        @Schema(description = "다음 요청에 사용할 커서 (마지막 항목의 ID)")
        Long nextCursor
) {
    public static <T> CursorResponse<T> of(List<T> content, boolean hasNext, Long nextCursor) {
        return new CursorResponse<>(content, hasNext, nextCursor);
    }
}