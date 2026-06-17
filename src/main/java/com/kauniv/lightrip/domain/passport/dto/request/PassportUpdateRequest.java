package com.kauniv.lightrip.domain.passport.dto.request;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "여권 수정 요청 (위치/날짜 수정 불가)")
public record PassportUpdateRequest(

        @Schema(description = "사진 URL 목록 (최소 1장, 최대 5장)")
        @NotNull(message = "이미지는 필수입니다.")
        @Size(min = 1, max = 5, message = "이미지는 1장 이상 5장 이하여야 합니다.")
        List<@NotBlank(message = "이미지 URL은 비어있을 수 없습니다.") String> imageUrls,

        @Schema(description = "기록 내용")
        @NotBlank(message = "기록 내용은 필수입니다.")
        String content,

        @Schema(description = "위치명")
        @Size(max = 50)
        String spaceName,

        @Schema(description = "카테고리")
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @Schema(description = "권역 카테고리")
        @NotNull(message = "권역 카테고리는 필수입니다.")
        District districtCategory,

        @Schema(description = "공개 범위")
        @NotNull(message = "공개 범위는 필수입니다.")
        Visibility visibility,

        @Schema(description = "음악 제목")
        @Size(max = 100)
        String musicTitle,

        @Schema(description = "음악 아티스트")
        @Size(max = 100)
        String musicArtist,

        @Schema(description = "테마 색상 RGB 값. 생략 또는 null 허용. 예: \"255,87,51\"", nullable = true, example = "255,87,51")
        @Size(max = 30)
        String theme
) {}