package com.kauniv.lightrip.domain.passport.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Schema(description = "릴스형 피드 여권 응답")
public record FeedPassportResponse(
        Long passportId,
        Long writerUserId,
        String writerNickname,
        String writerFriendCode,
        String writerProfileImg,
        boolean isFriend,
        List<String> imageUrls,
        String content,
        String address,
        String spaceName,
        String district,
        District districtCategory,
        String districtDisplayName,
        Category category,
        String categoryDisplayName,
        LocalDate visitedAt,
        String musicTitle,
        String musicArtist,
        Long likeCount,
        Long scrapCount,
        Long popularityScore,
        boolean isLiked,
        boolean isScrapped,
        BigDecimal distanceKm,
        LocalDateTime createdAt,

        @Schema(description = "테마 색상 RGB 값. 없으면 null", example = "255,87,51", nullable = true)
        String theme
) {
    public static FeedPassportResponse of(
            Passport p,
            Set<Long> likedIds,
            Set<Long> scrappedIds,
            Set<Long> friendIds,
            BigDecimal distanceKm
    ) {
        long score = (p.getLikeCount() * 2) + (p.getScrapCount() * 3);

        return new FeedPassportResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getNickname(),
                p.getUser().getFriendCode(),
                p.getUser().getProfileImg(),
                friendIds.contains(p.getUser().getId()),
                p.getImages().stream().map(img -> img.getImageUrl()).toList(),
                p.getContent(),
                p.getAddress(),
                p.getSpaceName(),
                p.getDistrict(),
                p.getDistrictCategory(),
                p.getDistrictCategory().getDisplayName(),
                p.getCategory(),
                p.getCategory().getDisplayName(),
                p.getVisitedAt(),
                p.getMusicTitle(),
                p.getMusicArtist(),
                p.getLikeCount(),
                p.getScrapCount(),
                score,
                likedIds.contains(p.getId()),
                scrappedIds.contains(p.getId()),
                distanceKm,
                p.getCreatedAt(),
                p.getTheme()
        );
    }
}