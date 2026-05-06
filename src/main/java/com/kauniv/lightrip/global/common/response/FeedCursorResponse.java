package com.kauniv.lightrip.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "피드 커서 페이징 응답 (인기 점수 + ID 이중 커서)")
public record FeedCursorResponse<T>(
        List<T> content,
        boolean hasNext,
        Long nextCursor,
        Long nextCursorScore
) {
    public static <T> FeedCursorResponse<T> of(List<T> content, boolean hasNext,
                                               Long nextCursor, Long nextCursorScore) {
        return new FeedCursorResponse<>(content, hasNext, nextCursor, nextCursorScore);
    }
}