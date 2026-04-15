// domain/passport/dto/request/PassportUpdateRequest.java
package com.kauniv.lightrip.domain.passport.dto.request;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "여권 수정 요청 (위치/날짜는 수정 불가)")
public record PassportUpdateRequest(

        @Schema(description = "사진 URL")
        @NotBlank String imageUrl,

        @Schema(description = "기록 내용")
        @NotBlank String content,

        @Schema(description = "위치명")
        @Size(max = 50) String spaceName,

        @Schema(description = "카테고리")
        @NotNull Category category,

        @Schema(description = "권역 카테고리")
        @NotNull District districtCategory,

        @Schema(description = "공개 범위")
        @NotNull Visibility visibility,

        @Schema(description = "음악 제목")
        @Size(max = 100) String musicTitle,

        @Schema(description = "음악 아티스트")
        @Size(max = 100) String musicArtist
) {}