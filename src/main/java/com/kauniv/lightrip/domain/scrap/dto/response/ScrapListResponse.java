package com.kauniv.lightrip.domain.scrap.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "스크랩 목록 항목")
public record ScrapListResponse(

        @Schema(description = "스크랩 ID (커서로 사용)")
        Long scrapId,

        @Schema(description = "스크랩 생성일시")
        LocalDateTime scrapCreatedAt,

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
    public static ScrapListResponse from(Scrap scrap) {
        Passport p = scrap.getPassport();

        String thumbnail = p.getImages().isEmpty()
                ? null
                : p.getImages().get(0).getImageUrl();

        return new ScrapListResponse(
                scrap.getId(),
                scrap.getCreatedAt(),
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