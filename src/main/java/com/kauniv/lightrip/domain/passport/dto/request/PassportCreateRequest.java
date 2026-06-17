package com.kauniv.lightrip.domain.passport.dto.request;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "여권 등록 요청")
public record PassportCreateRequest(

        @Schema(
                description = "[필수] 사진 URL 목록. **1~5장 필수**. S3 presigned URL 업로드 후 받은 CloudFront URL을 사용.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "[\"https://cdn.lightrip.cloud/passports/1/abc.jpg\",\"https://cdn.lightrip.cloud/passports/1/def.jpg\"]"
        )
        @NotNull(message = "이미지는 필수입니다.")
        @Size(min = 1, max = 5, message = "이미지는 1장 이상 5장 이하여야 합니다.")
        List<@NotBlank(message = "이미지 URL은 비어있을 수 없습니다.") String> imageUrls,

        @Schema(
                description = "[필수] 사용자가 작성한 최종 기록 내용. AI 초안을 받았다면 그것을 수정한 결과를 보냄.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "오늘 다녀온 카페, 분위기 너무 좋았다."
        )
        @NotBlank(message = "기록 내용은 필수입니다.")
        String content,

        @Schema(
                description = "[선택] AI가 생성한 초안 원본. 사용자가 수정 전 텍스트 — **학습 데이터로 보존됨**. AI 미사용 시 생략 또는 null.",
                example = "창가에 앉아 커피 향을 음미하며 보낸 한 시간..."
        )
        String draft,

        @Schema(
                description = "[선택] AI가 분류한 카테고리 초기값. 사용자가 `category`를 바꿔도 보존 — 모델 개선 분석용. AI 미사용 시 생략 또는 null.",
                example = "CAFE"
        )
        Category aiCategory,

        @Schema(
                description = "[필수] 위도. 소수점 7자리까지.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "37.5547"
        )
        @NotNull(message = "위도는 필수입니다.")
        BigDecimal latitude,

        @Schema(
                description = "[필수] 경도. 소수점 7자리까지.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "126.9223"
        )
        @NotNull(message = "경도는 필수입니다.")
        BigDecimal longitude,

        @Schema(
                description = "[필수] 전체 주소 문자열. 최대 50자.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "서울특별시 마포구 와우산로 94"
        )
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 50)
        String address,

        @Schema(
                description = "[필수] 방문 날짜. `YYYY-MM-DD` 형식. **오늘 또는 과거**만 허용 (미래 불가).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2026-05-20"
        )
        @NotNull(message = "방문 날짜는 필수입니다.")
        @PastOrPresent(message = "방문 날짜는 미래일 수 없습니다.")
        LocalDate visitedAt,

        @Schema(
                description = "[선택] 행정구역 표시명. UI 노출용. 생략 가능. 최대 50자.",
                example = "마포구"
        )
        @Size(max = 50)
        String district,

        @Schema(
                description = "[선택] 위치명 (장소 이름). 최대 50자. 생략 가능.",
                example = "안녕커피"
        )
        @Size(max = 50)
        String spaceName,

        @Schema(
                description = "[필수] 카테고리 (사용자 최종 선택값). CAFE/RESTAURANT/BAR/CULTURE/ACTIVITY/SHOPPING/NATURE/ETC 중 하나.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "CAFE"
        )
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @Schema(
                description = "[필수] 권역 카테고리 enum. 서울 25구 + 경기 31시군 중 하나 (예: MAPO, GANGNAM, SUWON).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "MAPO"
        )
        @NotNull(message = "권역 카테고리는 필수입니다.")
        District districtCategory,

        @Schema(
                description = "[선택] 공개 범위. 생략 시 **PUBLIC**으로 저장. PUBLIC(전체 공개) / FRIENDS_ONLY(친구만) / PRIVATE(나만).",
                example = "PUBLIC"
        )
        Visibility visibility,

        @Schema(
                description = "[선택] 함께 듣던 음악 제목. 최대 100자.",
                example = "Headphones On"
        )
        @Size(max = 100)
        String musicTitle,

        @Schema(
                description = "[선택] 함께 듣던 음악 아티스트. 최대 100자.",
                example = "Addison Rae"
        )
        @Size(max = 100)
        String musicArtist,

        @Schema(
                description = "[선택] 팀 여권으로 등록할 경우 팀 ID. **개인 여권이면 생략 또는 `null`**. " +
                        "⚠️ `0`은 허용 안 됨 (존재하지 않는 팀으로 간주되어 TEAM_NOT_FOUND). " +
                        "지정한 팀의 멤버가 아니면 403 PASSPORT_FORBIDDEN.",
                nullable = true,
                example = "null"
        )
        Long teamId,

        @Schema(
                description = "[선택] 테마 색상 RGB 값. 생략 또는 null 허용. 예: \"255,87,51\"",
                nullable = true,
                example = "255,87,51"
        )
        @Size(max = 30)
        String theme
) {
        public Visibility visibilityOrDefault() {
                return visibility == null ? Visibility.PUBLIC : visibility;
        }
}
