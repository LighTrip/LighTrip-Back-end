package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "지도 불빛 좌표 응답")
public record LightResponse(

        @Schema(description = "여권 ID")
        Long passportId,

        @Schema(description = "위도")
        BigDecimal latitude,

        @Schema(description = "경도")
        BigDecimal longitude,

        @Schema(description = "카테고리")
        Category category,

        @Schema(description = "지역 카테고리")
        District districtCategory,

        @Schema(description = "위치명")
        String spaceName,

        @Schema(description = "대표 이미지 URL (썸네일)")
        String thumbnailUrl,

        @Schema(description = "방문 날짜")
        LocalDate visitedAt,

        @Schema(description = "좋아요 수")
        Long likeCount,

        @Schema(description = "스크랩 수")
        Long scrapCount
) {
    public static LightResponse from(Passport p) {
        String thumbnail = p.getImages().isEmpty()
                ? null
                : p.getImages().get(0).getImageUrl();

        return new LightResponse(
                p.getId(),
                p.getLatitude(),
                p.getLongitude(),
                p.getCategory(),
                p.getDistrictCategory(),
                p.getSpaceName(),
                thumbnail,
                p.getVisitedAt(),
                p.getLikeCount(),
                p.getScrapCount()
        );
    }
}