package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "지도 불빛 좌표 응답 (단일 여권 또는 클러스터)")
public record LightResponse(

        @Schema(description = "여권 ID (클러스터인 경우 null)")
        Long passportId,

        @Schema(description = "위도 (클러스터인 경우 null)")
        BigDecimal latitude,

        @Schema(description = "경도 (클러스터인 경우 null)")
        BigDecimal longitude,

        @Schema(description = "카테고리 (클러스터인 경우 null)")
        Category category,

        @Schema(description = "지역 카테고리 (클러스터인 경우 null)")
        District districtCategory,

        @Schema(description = "위치명 (클러스터인 경우 null)")
        String spaceName,

        @Schema(description = "대표 이미지 URL (썸네일, 클러스터인 경우 null)")
        String thumbnailUrl,

        @Schema(description = "방문 날짜 (클러스터인 경우 null)")
        LocalDate visitedAt,

        @Schema(description = "좋아요 수 (클러스터인 경우 null)")
        Long likeCount,

        @Schema(description = "스크랩 수 (클러스터인 경우 null)")
        Long scrapCount,

        @Schema(description = "클러스터 여부 (true면 묶인 셀, false면 단일 여권)")
        boolean isCluster,

        @Schema(description = "셀 내 여권 수 (단일이면 1)")
        long count,

        @Schema(description = "셀 대표 위도 (단일이면 해당 여권 좌표와 동일)")
        BigDecimal centerLatitude,

        @Schema(description = "셀 대표 경도 (단일이면 해당 여권 좌표와 동일)")
        BigDecimal centerLongitude
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
                p.getScrapCount(),
                false,
                1L,
                p.getLatitude(),
                p.getLongitude()
        );
    }

    public static LightResponse cluster(long count, BigDecimal centerLatitude, BigDecimal centerLongitude) {
        return new LightResponse(
                null, null, null, null, null, null, null, null, null, null,
                true,
                count,
                centerLatitude,
                centerLongitude
        );
    }
}
