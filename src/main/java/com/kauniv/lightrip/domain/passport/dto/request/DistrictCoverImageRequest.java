package com.kauniv.lightrip.domain.passport.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지역 커버 이미지 변경 요청")
public record DistrictCoverImageRequest(

        @Schema(description = "새 커버로 지정할 여권 이미지 ID", example = "42")
        @NotNull(message = "여권 이미지 ID는 필수입니다.")
        Long passportImageId
) {}
