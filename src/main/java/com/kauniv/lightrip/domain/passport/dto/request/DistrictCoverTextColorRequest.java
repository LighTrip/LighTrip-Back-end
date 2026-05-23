package com.kauniv.lightrip.domain.passport.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "지역 커버 텍스트 색상 변경 요청")
public record DistrictCoverTextColorRequest(

        @Schema(description = "HEX 색상 코드 (#RRGGBB 6자리)", example = "#FFFFFF")
        @NotBlank(message = "텍스트 색상은 필수입니다.")
        @Pattern(
                regexp = "^#[A-Fa-f0-9]{6}$",
                message = "색상은 #RRGGBB 형식의 HEX 코드여야 합니다."
        )
        String textColor
) {}
