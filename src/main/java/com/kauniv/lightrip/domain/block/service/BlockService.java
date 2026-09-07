package com.kauniv.lightrip.domain.block.service;

import com.kauniv.lightrip.domain.block.dto.response.BlockedUserResponse;
import com.kauniv.lightrip.domain.block.entity.UserBlock;
import com.kauniv.lightrip.domain.block.repository.UserBlockRepository;
import com.kauniv.lightrip.domain.friend.repository.FriendRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 차단.
 *
 * <p>차단하면 (1) 두 사용자의 친구 관계·친구 요청이 즉시 해제되고,
 * (2) 이후 피드·추천·검색·단건 조회 등 모든 노출 경로에서 서로 보이지 않는다
 * (필터는 각 도메인 서비스가 {@link UserBlockRepository#findRelatedUserIds} / {@code existsBlockBetween} 로 수행).</p>
 *
 * <p>팀 내부(팀원 목록·팀 지도·실시간 위치)는 초대 기반 신뢰 관계이므로 차단을 적용하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.BLOCK_SELF_NOT_ALLOWED);
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // > 멱등 처리: 이미 차단한 상대면 아무것도 하지 않고 성공 응답.
        if (userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)) {
            return;
        }

        userBlockRepository.save(
                UserBlock.builder()
                        .blocker(blocker)
                        .blocked(blocked)
                        .build()
        );

        // > 부수효과: 친구 관계 / 대기 중인 친구 요청을 방향 무관하게 삭제.
        friendRepository.deleteFriendship(blockerId, blockedId);
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        UserBlock block = userBlockRepository.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOCK_NOT_FOUND));
        // > 차단만 해제. 이전 친구 관계는 복원하지 않는다.
        userBlockRepository.delete(block);
    }

    public CursorResponse<BlockedUserResponse> getMyBlocks(Long blockerId, Long cursor, int size) {
        List<UserBlock> blocks = (cursor == null)
                ? userBlockRepository.findMyBlocksFirst(blockerId, PageRequest.of(0, size + 1))
                : userBlockRepository.findMyBlocksAfterCursor(blockerId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = blocks.size() > size;
        if (hasNext) {
            blocks = blocks.subList(0, size);
        }

        List<BlockedUserResponse> content = blocks.stream()
                .map(BlockedUserResponse::from)
                .toList();

        Long nextCursor = hasNext ? blocks.get(blocks.size() - 1).getId() : null;

        return CursorResponse.of(content, hasNext, nextCursor);
    }
}
