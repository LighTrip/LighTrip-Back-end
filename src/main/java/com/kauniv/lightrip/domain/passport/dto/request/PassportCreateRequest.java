// domain/passport/dto/request/PassportCreateRequest.java
package com.kauniv.lightrip.domain.passport.dto.request;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "여권 등록 요청")
public record PassportCreateRequest(

        @Schema(description = "사진 URL", example = "https://cdn.lightrip.com/passport/abc.jpg")
        @NotBlank(message = "사진 URL은 필수입니다.")
        String imageUrl,

        @Schema(description = "기록 내용", example = "오늘 다녀온 카페, 분위기 너무 좋았다.")
        @NotBlank(message = "기록 내용은 필수입니다.")
        String content,

        @Schema(description = "위도", example = "37.5665")
        @NotNull(message = "위도는 필수입니다.")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.9780")
        @NotNull(message = "경도는 필수입니다.")
        BigDecimal longitude,

        @Schema(description = "전체 주소", example = "서울특별시 마포구 와우산로 123")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 50)
        String address,

        @Schema(description = "방문 날짜", example = "2026-04-15")
        @NotNull(message = "방문 날짜는 필수입니다.")
        @PastOrPresent(message = "방문 날짜는 미래일 수 없습니다.")
        LocalDate visitedAt,

        @Schema(description = "행정구역 (좌표 기반 자동 추출, 참고용)", example = "마포구")
        @Size(max = 50)
        String district,

        @Schema(description = "사용자가 입력한 위치명", example = "안녕커피")
        @Size(max = 50)
        String spaceName,

        @Schema(description = "카테고리", example = "CAFE")
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @Schema(description = "권역 카테고리", example = "MAPO")
        @NotNull(message = "권역 카테고리는 필수입니다.")
        District districtCategory,

        @Schema(description = "공개 범위 (기본 PUBLIC)", example = "PUBLIC",
                defaultValue = "PUBLIC")
        Visibility visibility,

        @Schema(description = "음악 제목", example = "Headphones On")
        @Size(max = 100)
        String musicTitle,

        @Schema(description = "음악 아티스트", example = "Addison Rae")
        @Size(max = 100)
        String musicArtist,

        @Schema(description = "팀 ID (개인 여권이면 null)", example = "null")
        Long teamId
) {
    public Visibility visibilityOrDefault() {
        return visibility == null ? Visibility.PUBLIC : visibility;
    }
}