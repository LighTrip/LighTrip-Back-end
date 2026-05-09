package com.kauniv.lightrip.domain.passport.dto.request;

import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "여권 공개 범위 변경 요청")
public record PassportVisibilityRequest(

        @Schema(description = "공개 범위 (PUBLIC / FRIENDS_ONLY / PRIVATE)", example = "PRIVATE")
        @NotNull(message = "공개 범위는 필수입니다.")
        Visibility visibility
) {}