package com.kauniv.lightrip.domain.like.dto.response;

import com.kauniv.lightrip.domain.like.entity.Like;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "좋아요 응답")
public record LikeResponse(
        Long likeId,
        Long passportId,
        LocalDateTime createdAt
) {
    public static LikeResponse from(Like like) {
        return new LikeResponse(
                like.getId(),
                like.getPassport().getId(),
                like.getCreatedAt()
        );
    }
}