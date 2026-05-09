package com.kauniv.lightrip.domain.friend.service;

import com.kauniv.lightrip.domain.friend.dto.request.FriendAction;
import com.kauniv.lightrip.domain.friend.dto.request.FriendRequestDto;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdateDto;
import com.kauniv.lightrip.domain.friend.dto.response.FriendPassportResponse;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponseDto;
import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final PassportRepository passportRepository;

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

        if (dto.action() == FriendAction.ACCEPT) {
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

    public List<FriendResponseDto> getFriends(Long userId) {
        List<Friend> friends = friendRepository.findAllFriends(userId);

        return friends.stream()
                .map(friend -> {
                    User target = friend.getRequester().getId().equals(userId)
                            ? friend.getReceiver()
                            : friend.getRequester();
                    return FriendResponseDto.from(friend, target);
                })
                .toList();
    }

    public List<FriendResponseDto> getPendingRequests(Long userId) {
        List<Friend> pendings = friendRepository.findPendingRequests(userId);

        return pendings.stream()
                .map(friend -> FriendResponseDto.from(friend, friend.getRequester()))
                .toList();
    }

    public FriendResponseDto searchByFriendCode(String friendCode) {
        User user = userRepository.findByFriendCode(friendCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new FriendResponseDto(
                null,
                user.getId(),
                user.getNickname(),
                user.getProfileImg(),
                user.getFriendCode(),
                null,
                null
        );
    }

    // 친구 여권 조회 — ACCEPTED 친구 관계 확인 후 PUBLIC 여권만 반환
    public List<FriendPassportResponse> getFriendPassports(Long currentUserId,
                                                           Long friendId,
                                                           Pageable pageable) {
        // 친구 관계 확인: isFriend()가 ACCEPTED 양방향 체크를 이미 수행
        if (!friendRepository.isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        // 대상 유저 존재 확인
        userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return passportRepository.findPublicPassportsByUserId(friendId, pageable)
                .stream()
                .map(FriendPassportResponse::from)
                .toList();
    }
}