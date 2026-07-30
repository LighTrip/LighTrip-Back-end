package com.kauniv.lightrip.global.ai.dto;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "AI 여권 초안 생성 요청")
public record AiDraftRequest(

        @Schema(
                description = "[필수] 장소명. 최대 50자.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "안녕커피"
        )
        @NotBlank(message = "장소명은 필수입니다.")
        @Size(max = 50)
        String spaceName,

        @Schema(
                description = "[필수] 카테고리. CAFE/RESTAURANT/BAR/CULTURE/ACTIVITY/SHOPPING/NATURE/ETC 중 하나. "
                        + "응답의 `category`로 그대로 되돌려준다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "CAFE"
        )
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @Schema(
                description = "[필수] 권역 카테고리 enum. 서울 25구 + 경기 시·군 (예: MAPO, GANGNAM, SUWON_PALDAL).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "MAPO"
        )
        @NotNull(message = "권역 카테고리는 필수입니다.")
        District districtCategory,

        @Schema(
                description = "[필수] 방문 날짜. `YYYY-MM-DD` 형식. **오늘 또는 과거**만 허용.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2026-05-20"
        )
        @NotNull(message = "방문 날짜는 필수입니다.")
        @PastOrPresent(message = "방문 날짜는 미래일 수 없습니다.")
        LocalDate visitedAt,

        @Schema(
                description = "[선택] 초안에 반영할 키워드. 최대 5개, 각 20자 이내. 생략 또는 null 허용.",
                nullable = true,
                example = "[\"창가 자리\",\"드립커피\"]"
        )
        @Size(max = 5, message = "키워드는 5개 이하여야 합니다.")
        List<@NotBlank(message = "키워드는 비어있을 수 없습니다.") @Size(max = 20) String> keywords,

        @Schema(
                description = "[선택] 방문 사진 URL. 넣으면 사진에 보이는 것까지 반영해서 초안을 만든다. "
                        + "**OpenAI 서버가 직접 받아가므로 공개 접근 가능한 https URL이어야 한다** "
                        + "(presigned URL 말고 CloudFront URL). 생략 또는 null 허용.",
                nullable = true,
                example = "https://cdn.lightrip.cloud/passports/sample.jpg"
        )
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^https://.*", message = "이미지 URL은 https로 시작해야 합니다.")
        String imageUrl
) {}
