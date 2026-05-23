package com.kauniv.lightrip.domain.passport.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "지역 커버 이미지 변경 요청")
public record DistrictCoverImageRequest(

        @Schema(description = "새 커버 이미지 URL", example = "https://cdn.lightrip.cloud/passports/1/abc.jpg")
        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 2048, message = "이미지 URL은 2048자를 넘을 수 없습니다.")
        String imageUrl
) {}
