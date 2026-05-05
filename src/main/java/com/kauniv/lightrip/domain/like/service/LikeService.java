package com.kauniv.lightrip.domain.like.service;

import com.kauniv.lightrip.domain.like.dto.response.LikeListResponse;
import com.kauniv.lightrip.domain.like.dto.response.LikeResponse;
import com.kauniv.lightrip.domain.like.entity.Like;
import com.kauniv.lightrip.domain.like.repository.LikeRepository;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PassportRepository passportRepository;
    private final UserRepository userRepository;

    // ========== 좋아요 등록 ==========
    @Transactional
    public LikeResponse create(Long userId, Long passportId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        if (likeRepository.existsByUser_IdAndPassport_Id(userId, passportId)) {
            throw new BusinessException(ErrorCode.LIKE_DUPLICATE);
        }

        Like like = Like.builder()
                .user(user)
                .passport(passport)
                .build();

        likeRepository.save(like);
        passport.increaseLikeCount();

        return LikeResponse.from(like);
    }

    // ========== 좋아요 취소 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Like like = likeRepository.findByUser_IdAndPassport_Id(userId, passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        Passport passport = like.getPassport();

        likeRepository.delete(like);
        passport.decreaseLikeCount();
    }

    // ========== 내 좋아요 목록 조회 ==========
    public CursorResponse<LikeListResponse> getMyLikes(Long userId, Long cursor, int size) {
        List<Like> likes = (cursor == null)
                ? likeRepository.findByUserIdFirst(userId, PageRequest.of(0, size + 1))
                : likeRepository.findByUserIdAfterCursor(userId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = likes.size() > size;

        if (hasNext) {
            likes = likes.subList(0, size);
        }

        List<LikeListResponse> content = likes.stream()
                .map(LikeListResponse::from)
                .toList();

        Long nextCursor = hasNext ? likes.get(likes.size() - 1).getId() : null;

        return CursorResponse.of(content, hasNext, nextCursor);
    }
}