package com.kauniv.lightrip.domain.friend.service;

import com.kauniv.lightrip.domain.friend.dto.request.FriendAction;
import com.kauniv.lightrip.domain.friend.dto.request.FriendRequest;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdate;
import com.kauniv.lightrip.domain.friend.dto.response.FriendPassportResponse;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponse;
import com.kauniv.lightrip.domain.friend.entity.Friend;
import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.passport.dto.response.DistrictResponse;
import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.domain.passport.repository.DistrictCoverRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.enums.District;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final PassportRepository passportRepository;
    private final DistrictCoverRepository districtCoverRepository;
    private final ScrapRepository scrapRepository;

    @Transactional
    public FriendResponse sendRequest(Long requesterId, FriendRequest dto) {
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
        return FriendResponse.from(friend, receiver);
    }

    @Transactional
    public FriendResponse handleRequest(Long userId, Long friendId, FriendStatusUpdate dto) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));

        if (!friend.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_RECEIVER);
        }

        if (dto.action() == FriendAction.ACCEPT) {
            friend.accept();
            return FriendResponse.from(friend, friend.getRequester());
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

    public List<FriendResponse> getFriends(Long userId) {
        List<Friend> friends = friendRepository.findAllFriends(userId);

        return friends.stream()
                .map(friend -> {
                    User target = friend.getRequester().getId().equals(userId)
                            ? friend.getReceiver()
                            : friend.getRequester();
                    return FriendResponse.from(friend, target);
                })
                .toList();
    }

    public List<FriendResponse> getPendingRequests(Long userId) {
        List<Friend> pendings = friendRepository.findPendingRequests(userId);

        return pendings.stream()
                .map(friend -> FriendResponse.from(friend, friend.getRequester()))
                .toList();
    }

    public FriendResponse searchByFriendCode(String friendCode) {
        User user = userRepository.findByFriendCode(friendCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new FriendResponse(
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
        if (!friendRepository.isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return passportRepository.findPublicPassportsByUserId(friendId, pageable)
                .stream()
                .map(FriendPassportResponse::from)
                .toList();
    }

    // 친구 지도 조회 — PUBLIC 여권 기준 district 목록 반환
    public List<DistrictResponse> getFriendDistricts(Long currentUserId, Long friendId) {
        if (!friendRepository.isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Object[]> counts = passportRepository.countPublicByUserIdGroupByDistrict(friendId);

        Map<District, String> coverMap = districtCoverRepository.findAllByUser_Id(friendId).stream()
                .collect(Collectors.toMap(
                        DistrictCover::getDistrictCategory,
                        cover -> cover.getPassportImage().getImageUrl()
                ));

        return counts.stream()
                .map(row -> {
                    District district = (District) row[0];
                    Long count = (Long) row[1];
                    String thumbnailUrl = coverMap.get(district);
                    return DistrictResponse.of(district, count, thumbnailUrl);
                })
                .toList();
    }

    // 추천 친구 조회 — 내가 스크랩한 여권 작성자 중 아직 친구가 아닌 유저 최대 4명 랜덤 반환
    public List<FriendResponse> getRecommendedFriends(Long userId) {
        // 1. 내가 스크랩한 여권의 작성자 ID 목록 (본인 제외)
        List<Long> candidateIds = scrapRepository.findScrapedPassportOwnerIds(userId);

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // 2. 이미 친구이거나 요청 중인 유저 제외
        Set<Long> existingFriendIds = friendRepository.findAllFriends(userId).stream()
                .map(f -> f.getRequester().getId().equals(userId)
                        ? f.getReceiver().getId()
                        : f.getRequester().getId())
                .collect(Collectors.toSet());

        // PENDING 상태도 제외
        Set<Long> pendingIds = friendRepository.findPendingRequests(userId).stream()
                .map(f -> f.getRequester().getId())
                .collect(Collectors.toSet());

        List<Long> filteredIds = candidateIds.stream()
                .filter(id -> !existingFriendIds.contains(id) && !pendingIds.contains(id))
                .collect(Collectors.toList());

        // 3. 랜덤 셔플 후 최대 4명 추출
        Collections.shuffle(filteredIds);
        List<Long> selectedIds = filteredIds.stream().limit(4).toList();

        if (selectedIds.isEmpty()) {
            return List.of();
        }

        // 4. User 정보 조회 후 응답 변환
        return userRepository.findAllById(selectedIds).stream()
                .map(user -> new FriendResponse(
                        null,
                        user.getId(),
                        user.getNickname(),
                        user.getProfileImg(),
                        user.getFriendCode(),
                        null,
                        null
                ))
                .toList();
    }
}