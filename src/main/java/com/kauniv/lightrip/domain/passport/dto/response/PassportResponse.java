package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.entity.PassportImage;
import com.kauniv.lightrip.domain.passport.entity.Stamp;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "여권 응답")
public record PassportResponse(
        Long passportId,
        Long userId,
        Long teamId,
        List<String> imageUrls,
        String content,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        LocalDate visitedAt,
        String district,
        String spaceName,
        Category category,
        District districtCategory,
        Visibility visibility,
        String musicTitle,
        String musicArtist,
        Long likeCount,
        Long scrapCount,
        List<String> stampUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        @Schema(description = "지역 커버 ID (커버 변경 시 사용). 커버가 없으면 null")
        Long coverId,

        @Schema(description = "테마 색상 RGB 값. 없으면 null", example = "255,87,51", nullable = true)
        String theme
) {
    public static PassportResponse from(Passport p) {
        return from(p, null);
    }

    public static PassportResponse from(Passport p, Long coverId) {
        List<String> urls = p.getImages().stream()
                .map(PassportImage::getImageUrl)
                .toList();

        List<String> stamps = p.getStamps().stream()
                .map(Stamp::getImageUrl)
                .toList();

        return new PassportResponse(
                p.getId(),
                p.getUser().getId(),
                p.getTeam() == null ? null : p.getTeam().getId(),
                urls,
                p.getContent(),
                p.getLatitude(),
                p.getLongitude(),
                p.getAddress(),
                p.getVisitedAt(),
                p.getDistrict(),
                p.getSpaceName(),
                p.getCategory(),
                p.getDistrictCategory(),
                p.getVisibility(),
                p.getMusicTitle(),
                p.getMusicArtist(),
                p.getLikeCount(),
                p.getScrapCount(),
                stamps,
                p.getCreatedAt(),
                p.getUpdatedAt(),
                coverId,
                p.getTheme()
        );
    }
}