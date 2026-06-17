package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "여권 목록 항목 (요약 정보)")
public record PassportListResponse(

        @Schema(description = "여권 ID", example = "1")
        Long passportId,

        @Schema(description = "대표 이미지 URL", example = "https://cdn.lightrip.com/img/1.jpg")
        String thumbnailUrl,

        @Schema(description = "위치명", example = "렉터스라운지 홍대")
        String spaceName,

        @Schema(description = "지역 카테고리", example = "MAPO")
        District districtCategory,

        @Schema(description = "지역명", example = "마포구")
        String districtDisplayName,

        @Schema(description = "카테고리", example = "CAFE")
        Category category,

        @Schema(description = "카테고리명", example = "카페")
        String categoryDisplayName,

        @Schema(description = "방문 날짜", example = "2026-03-30")
        LocalDate visitedAt,

        @Schema(description = "공개 범위", example = "PUBLIC")
        Visibility visibility,

        @Schema(description = "좋아요 수", example = "12")
        Long likeCount,

        @Schema(description = "스크랩 수", example = "5")
        Long scrapCount,

        @Schema(description = "테마 색상 RGB 값. 없으면 null", example = "255,87,51", nullable = true)
        String theme
) {
    public static PassportListResponse from(Passport p) {
        String thumbnail = p.getImages().isEmpty()
                ? null
                : p.getImages().get(0).getImageUrl();

        return new PassportListResponse(
                p.getId(),
                thumbnail,
                p.getSpaceName(),
                p.getDistrictCategory(),
                p.getDistrictCategory().getDisplayName(),
                p.getCategory(),
                p.getCategory().getDisplayName(),
                p.getVisitedAt(),
                p.getVisibility(),
                p.getLikeCount(),
                p.getScrapCount(),
                p.getTheme()
        );
    }
}