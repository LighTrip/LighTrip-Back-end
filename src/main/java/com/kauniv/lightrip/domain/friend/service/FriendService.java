package com.kauniv.lightrip.domain.friend.service;

import com.kauniv.lightrip.domain.friend.dto.request.FriendRequestDto;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdateDto;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponseDto;
import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional
    public FriendResponseDto sendRequest(Long requesterId, FriendRequestDto dto) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User receiver = userRepository.findByFriendCode(dto.friendCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (requester.getId().equals(receiver.getId())) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_REQUEST);
        }

        if (friendRepository.existsFriendship(requester.getId(), receiver.getId())) {
            throw new BusinessException(ErrorCode.FRIEND_ALREADY_REQUESTED);
        }

        Friend friend = Friend.builder()
                .requester(requester)
                .receiver(receiver)
                .status(Friend.Status.PENDING)
                .build();

        friendRepository.save(friend);
        return FriendResponseDto.from(friend, receiver);
    }

    @Transactional
    public FriendResponseDto handleRequest(Long userId, Long friendId, FriendStatusUpdateDto dto) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));

        if (!friend.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_RECEIVER);
        }

        if ("ACCEPT".equalsIgnoreCase(dto.action())) {
            friend.accept();
            return FriendResponseDto.from(friend, friend.getRequester());
        } else {
            friendRepository.delete(friend);
            return null;
        }
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));

        if (!friend.getRequester().getId().equals(userId)
                && !friend.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        friendRepository.delete(friend);
    }
}