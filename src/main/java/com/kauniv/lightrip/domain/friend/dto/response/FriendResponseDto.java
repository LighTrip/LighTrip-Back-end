package com.kauniv.lightrip.domain.friend.dto.response;

import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.user.entity.User;

import java.time.LocalDateTime;

public record FriendResponseDto(
        Long friendId,
        Long userId,
        String nickname,
        String profileImg,
        String friendCode,
        String status,
        LocalDateTime createdAt
) {
    public static FriendResponseDto from(Friend friend, User target) {
        return new FriendResponseDto(
                friend.getId(),
                target.getId(),
                target.getNickname(),
                target.getProfileImg(),
                target.getFriendCode(),
                friend.getStatus().name(),
                friend.getCreatedAt()
        );
    }
}