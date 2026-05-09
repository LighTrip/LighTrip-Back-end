package com.kauniv.lightrip.domain.friend.dto.response;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.global.enums.Category;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FriendPassportResponse {

    private Long passportId;
    private String spaceName;
    private String address;
    private Category category;
    private LocalDate visitedAt;
    private String content;
    private List<String> imageUrls;
    private Long likeCount;
    private Long scrapCount;

    public static FriendPassportResponse from(Passport passport) {
        return FriendPassportResponse.builder()
                .passportId(passport.getId())
                .spaceName(passport.getSpaceName())
                .address(passport.getAddress())
                .category(passport.getCategory())
                .visitedAt(passport.getVisitedAt())
                .content(passport.getContent())
                .imageUrls(passport.getImages().stream()
                        .map(img -> img.getImageUrl())
                        .toList())
                .likeCount(passport.getLikeCount())
                .scrapCount(passport.getScrapCount())
                .build();
    }
}