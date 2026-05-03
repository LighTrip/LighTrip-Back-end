package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기록 지역 응답")
public record DistrictResponse(

        @Schema(description = "지역 카테고리", example = "MAPO")
        District districtCategory,

        @Schema(description = "지역명", example = "마포구")
        String displayName,

        @Schema(description = "해당 지역 여권 수")
        Long passportCount,

        @Schema(description = "대표 이미지 URL")
        String thumbnailUrl
) {
    public static DistrictResponse of(District district, Long count, String thumbnailUrl) {
        return new DistrictResponse(
                district,
                district.getDisplayName(),
                count,
                thumbnailUrl
        );
    }
}