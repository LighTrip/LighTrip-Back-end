package com.kauniv.lightrip.domain.friend.dto.response;

import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record FriendResponse(
        Long friendId,
        Long userId,
        String nickname,
        String profileImg,
        String friendCode,
        String location,
        String status,
        LocalDateTime createdAt,
        Long passportCount,
        List<MutualFriendInfo> mutualFriends
) {
    public static FriendResponse from(Friend friend, User target) {
        return new FriendResponse(
                friend.getId(),
                target.getId(),
                target.getNickname(),
                target.getProfileImg(),
                target.getFriendCode(),
                target.getLocation(),
                friend.getStatus().name(),
                friend.getCreatedAt(),
                null,
                null
        );
    }

    public static FriendResponse from(Friend friend, User target,
                                      Long passportCount,
                                      List<MutualFriendInfo> mutualFriends) {
        return new FriendResponse(
                friend.getId(),
                target.getId(),
                target.getNickname(),
                target.getProfileImg(),
                target.getFriendCode(),
                target.getLocation(),
                friend.getStatus().name(),
                friend.getCreatedAt(),
                passportCount,
                mutualFriends
        );
    }

    public static FriendResponse ofUser(User user,
                                        Long passportCount,
                                        List<MutualFriendInfo> mutualFriends) {
        return new FriendResponse(
                null,
                user.getId(),
                user.getNickname(),
                user.getProfileImg(),
                user.getFriendCode(),
                user.getLocation(),
                null,
                null,
                passportCount,
                mutualFriends
        );
    }

    public record MutualFriendInfo(
            Long userId,
            String nickname,
            String profileImg
    ) {
        public static MutualFriendInfo from(User user) {
            return new MutualFriendInfo(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImg()
            );
        }

        public static MutualFriendInfo fromRow(Object[] row) {
            return new MutualFriendInfo(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2]
            );
        }
    }
}