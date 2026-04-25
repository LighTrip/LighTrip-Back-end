// domain/passport/service/PassportService.java
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassportService {

    private final PassportRepository passportRepository;
    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    // ========== 등록 ==========
    @Transactional
    public PassportResponse create(Long userId, PassportCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 중복 체크 (같은 사용자 + 같은 좌표 + 같은 날짜)
        if (passportRepository.existsByUser_IdAndLatitudeAndLongitudeAndVisitedAt(
                userId, req.latitude(), req.longitude(), req.visitedAt())) {
            throw new BusinessException(ErrorCode.PASSPORT_DUPLICATE);
        }

        // 팀 모드 검증: teamId가 있으면 해당 팀의 멤버여야 함
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
                .imageUrl(req.imageUrl())
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

        return PassportResponse.from(passportRepository.save(passport));
    }

    // ========== 수정 ==========
    @Transactional
    public PassportResponse update(Long userId, Long passportId, PassportUpdateRequest req) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        // 권한: 개인 여권 → 본인만 / 팀 여권 → 팀원 누구나
        validateUpdatePermission(passport, userId);

        passport.update(
                req.imageUrl(), req.content(), req.spaceName(),
                req.category(), req.districtCategory(), req.visibility(),
                req.musicTitle(), req.musicArtist()
        );

        return PassportResponse.from(passport);
    }

    // ========== 삭제 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        // 권한: 개인/팀 여권 모두 → 작성자(본인)만 삭제 가능
        if (!passport.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
        }

        // 연관 스크랩 먼저 삭제 후 여권 삭제
        scrapRepository.deleteAllByPassportId(passportId);
        passportRepository.delete(passport);
    }

    // ========== 권한 검증 헬퍼 ==========
    private void validateUpdatePermission(Passport passport, Long userId) {
        if (passport.isTeamPassport()) {
            // 팀 여권: 팀원이면 누구나 수정 가능
            boolean isMember = teamMemberRepository.existsByTeam_IdAndUser_Id(
                    passport.getTeam().getId(), userId
            );
            if (!isMember) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        } else {
            // 개인 여권: 본인만
            if (!passport.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.PASSPORT_FORBIDDEN);
            }
        }
    }
}