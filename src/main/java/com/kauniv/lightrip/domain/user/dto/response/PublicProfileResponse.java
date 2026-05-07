package com.kauniv.lightrip.domain.user.dto.response;

import com.kauniv.lightrip.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicProfileResponse {

    private Long userId;
    private String nickname;
    private String profileImg;

    public static PublicProfileResponse from(User user) {
        return PublicProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .build();
    }
}
