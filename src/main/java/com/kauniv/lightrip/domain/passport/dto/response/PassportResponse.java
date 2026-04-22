// domain/passport/dto/response/PassportResponse.java
package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import com.kauniv.lightrip.global.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "여권 응답")
public record PassportResponse(
        Long passportId,
        Long userId,
        Long teamId,
        String imageUrl,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PassportResponse from(Passport p) {
        return new PassportResponse(
                p.getId(),
                p.getUser().getId(),
                p.getTeam() == null ? null : p.getTeam().getId(),
                p.getImageUrl(),
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
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}