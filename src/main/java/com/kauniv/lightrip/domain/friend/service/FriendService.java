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

        if (friends.isEmpty()) {
            return List.of();
        }

        // 친구 유저 ID 목록 추출
        List<Long> targetIds = friends.stream()
                .map(f -> f.getRequester().getId().equals(userId)
                        ? f.getReceiver().getId()
                        : f.getRequester().getId())
                .toList();

        // 도장 수 일괄 조회
        Map<Long, Long> passportCountMap = passportRepository.countByUserIds(targetIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return friends.stream()
                .map(friend -> {
                    User target = friend.getRequester().getId().equals(userId)
                            ? friend.getReceiver()
                            : friend.getRequester();

                    Long passportCount = passportCountMap.getOrDefault(target.getId(), 0L);

                    // 공통 친구 목록 조회
                    List<FriendResponse.MutualFriendInfo> mutualFriends =
                            friendRepository.findMutualFriends(userId, target.getId()).stream()
                                    .map(FriendResponse.MutualFriendInfo::fromRow)
                                    .toList();

                    return FriendResponse.from(friend, target, passportCount, mutualFriends);
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

        return FriendResponse.ofUser(user, null, null);
    }

    public List<FriendPassportResponse> getFriendPassports(Long currentUserId,
                                                           Long friendId,
                                                           District district,
                                                           Pageable pageable) {
        if (!friendRepository.isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return passportRepository.findPublicPassportsByUserId(friendId, district, pageable)
                .stream()
                .map(FriendPassportResponse::from)
                .toList();
    }

    public List<DistrictResponse> getFriendDistricts(Long currentUserId, Long friendId) {
        if (!friendRepository.isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_MEMBER);
        }

        userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Object[]> counts = passportRepository.countPublicByUserIdGroupByDistrict(friendId);

        Map<District, DistrictCover> coverMap = districtCoverRepository.findAllByUser_Id(friendId).stream()
                .collect(Collectors.toMap(DistrictCover::getDistrictCategory, c -> c));

        return counts.stream()
                .map(row -> {
                    District district = (District) row[0];
                    Long count = (Long) row[1];
                    DistrictCover cover = coverMap.get(district);
                    String thumbnailUrl = cover != null ? cover.getImageUrl() : null;
                    String textColor = cover != null ? cover.getTextColor() : null;
                    return DistrictResponse.of(district, count, thumbnailUrl, textColor);
                })
                .toList();
    }

    public List<FriendResponse> getRecommendedFriends(Long userId) {
        List<Long> candidateIds = scrapRepository.findScrapedPassportOwnerIds(userId);

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Set<Long> existingFriendIds = friendRepository.findAllFriends(userId).stream()
                .map(f -> f.getRequester().getId().equals(userId)
                        ? f.getReceiver().getId()
                        : f.getRequester().getId())
                .collect(Collectors.toSet());

        Set<Long> pendingIds = friendRepository.findPendingRequests(userId).stream()
                .map(f -> f.getRequester().getId())
                .collect(Collectors.toSet());

        List<Long> filteredIds = candidateIds.stream()
                .filter(id -> !existingFriendIds.contains(id) && !pendingIds.contains(id))
                .collect(Collectors.toList());

        Collections.shuffle(filteredIds);
        List<Long> selectedIds = filteredIds.stream().limit(4).toList();

        if (selectedIds.isEmpty()) {
            return List.of();
        }

        // 도장 수 일괄 조회
        Map<Long, Long> passportCountMap = passportRepository.countByUserIds(selectedIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return userRepository.findAllById(selectedIds).stream()
                .map(user -> {
                    Long passportCount = passportCountMap.getOrDefault(user.getId(), 0L);

                    // 공통 친구 목록 조회
                    List<FriendResponse.MutualFriendInfo> mutualFriends =
                            friendRepository.findMutualFriends(userId, user.getId()).stream()
                                    .map(FriendResponse.MutualFriendInfo::fromRow)
                                    .toList();

                    return FriendResponse.ofUser(user, passportCount, mutualFriends);
                })
                .toList();
    }
}