package com.kauniv.lightrip.domain.passport.service;

import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
import com.kauniv.lightrip.domain.passport.dto.response.PassportResponse;
import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import com.kauniv.lightrip.domain.team.repository.TeamRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kauniv.lightrip.domain.friend.repository.FriendRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassportService {

    private final PassportRepository passportRepository;
    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final FriendRepository friendRepository;

    // ========== 등록 ==========
    @Transactional
    public PassportResponse create(Long userId, PassportCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (passportRepository.existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
                userId, req.latitude(), req.longitude(), req.visitedAt())) {
            throw new BusinessException(ErrorCode.PASSPORT_DUPLICATE);
        }

        Team team = null;
        if (req.teamId() != null) {
            team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(team.getId(), userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }

        Passport passport = Passport.builder()
                .user(user)
                .team(team)
                .content(req.content())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .address(req.address())
                .visitedAt(req.visitedAt())
                .district(req.district())
                .spaceName(req.spaceName())
                .category(req.category())
                .districtCategory(req.districtCategory())
                .visibility(req.visibilityOrDefault())
                .musicTitle(req.musicTitle())
                .musicArtist(req.musicArtist())
                .build();

        passportRepository.save(passport);
        passport.replaceImages(req.imageUrls());

        return PassportResponse.from(passport);
    }

    // ========== 수정 ==========
    @Transactional
    public PassportResponse update(Long userId, Long passportId, PassportUpdateRequest req) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        validateUpdatePermission(passport, userId);

        passport.update(
                req.content(), req.spaceName(),
                req.category(), req.districtCategory(), req.visibility(),
                req.musicTitle(), req.musicArtist()
        );

        passport.replaceImages(req.imageUrls());

        return PassportResponse.from(passport);
    }

    // ========== 삭제 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        if (!passport.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }

        scrapRepository.deleteAllByPassportId(passportId);
        passportRepository.delete(passport);
    }

    // ========== 수정 권한 검증 ==========
    private void validateUpdatePermission(Passport passport, Long userId) {
        if (passport.isTeamPassport()) {
            boolean isMember = teamMemberRepository.existsByTeam_IdAndUser_Id(
                    passport.getTeam().getId(), userId
            );
            if (!isMember) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        } else {
            if (!passport.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }
    }

    // ========== 상세 조회 ==========
    public PassportResponse getPassport(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        validateReadPermission(passport, userId);

        return PassportResponse.from(passport);
    }

    // ========== 조회 권한 검증 (Visibility) ==========
    private void validateReadPermission(Passport passport, Long userId) {
        if (passport.isOwnedBy(userId)) {
            return;
        }

        if (passport.isTeamPassport()) {
            boolean isMember = teamMemberRepository.existsByTeam_IdAndUser_Id(
                    passport.getTeam().getId(), userId
            );
            if (isMember) {
                return;
            }
        }

        switch (passport.getVisibility()) {
            case PUBLIC -> {
                return;
            }
            case FRIENDS_ONLY -> {
                Long ownerId = passport.getUser().getId();
                if (friendRepository.isFriend(userId, ownerId)) {
                    return;
                }
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
            case PRIVATE -> throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }
    }
    public Passport getPassportWithReadCheck(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        validateReadPermission(passport, userId);
        return passport;
    }
}

