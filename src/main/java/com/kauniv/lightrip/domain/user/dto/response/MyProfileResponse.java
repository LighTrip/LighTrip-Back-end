package com.kauniv.lightrip.domain.user.dto.response;

import com.kauniv.lightrip.domain.user.entity.CurrentMode;
import com.kauniv.lightrip.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyProfileResponse {

    private Long userId;
    private String nickname;
    private String email;
    private String profileImg;
    private String friendCode;
    private String location;
    private String bio;
    private CurrentMode currentMode;
    private LocalDateTime createdAt;
    private Stats stats;

    @Getter
    @Builder
    public static class Stats {
        private long passportCount;
        private long friendCount;
        private long likeCount;
        private long scrapCount;
    }

    public static MyProfileResponse from(User user, Stats stats) {
        return MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImg(user.getProfileImg())
                .friendCode(user.getFriendCode())
                .location(user.getLocation())
                .bio(user.getBio())
                .currentMode(user.getCurrentMode())
                .createdAt(user.getCreatedAt())
                .stats(stats)
                .build();
    }
}