package com.kauniv.lightrip.domain.like.dto.response;

import com.kauniv.lightrip.domain.like.entity.Like;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "좋아요 목록 항목")
public record LikeListResponse(

        @Schema(description = "좋아요 ID (커서로 사용)")
        Long likeId,

        @Schema(description = "좋아요 생성일시")
        LocalDateTime likeCreatedAt,

        @Schema(description = "여권 ID")
        Long passportId,

        @Schema(description = "대표 이미지 URL (첫 번째 이미지)")
        String thumbnailUrl,

        @Schema(description = "기록 내용")
        String content,

        @Schema(description = "주소")
        String address,

        @Schema(description = "위치명")
        String spaceName,

        @Schema(description = "좋아요 수")
        Long likeCount,

        @Schema(description = "스크랩 수")
        Long scrapCount
) {
    public static LikeListResponse from(Like like) {
        Passport p = like.getPassport();

        String thumbnail = p.getImages().isEmpty()
                ? null
                : p.getImages().get(0).getImageUrl();

        return new LikeListResponse(
                like.getId(),
                like.getCreatedAt(),
                p.getId(),
                thumbnail,
                p.getContent(),
                p.getAddress(),
                p.getSpaceName(),
                p.getLikeCount(),
                p.getScrapCount()
        );
    }
}