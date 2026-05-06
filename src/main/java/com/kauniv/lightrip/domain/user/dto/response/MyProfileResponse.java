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
    private CurrentMode currentMode;
    private LocalDateTime createdAt;

    public static MyProfileResponse from(User user) {
        return MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImg(user.getProfileImg())
                .currentMode(user.getCurrentMode())
                .createdAt(user.getCreatedAt())
                .build();
    }
}