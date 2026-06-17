package com.kauniv.lightrip.domain.passport.service;

import com.kauniv.lightrip.domain.passport.dto.request.DistrictCoverImageRequest;
import com.kauniv.lightrip.domain.passport.dto.request.DistrictCoverTextColorRequest;
import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.domain.passport.repository.DistrictCoverRepository;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistrictCoverService {

    private final DistrictCoverRepository districtCoverRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public void updateTextColor(Long userId, Long coverId, DistrictCoverTextColorRequest req) {
        DistrictCover cover = districtCoverRepository.findById(coverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_COVER_NOT_FOUND));

        validatePermission(cover, userId);

        cover.changeTextColor(req.textColor());
    }

    @Transactional
    public void updateCoverImage(Long userId, Long coverId, DistrictCoverImageRequest req) {
        DistrictCover cover = districtCoverRepository.findById(coverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_COVER_NOT_FOUND));

        validatePermission(cover, userId);

        cover.changeCoverImage(req.imageUrl());
    }

    // > 팀 커버는 팀 멤버 누구나, 개인 커버는 작성자 본인만 변경 가능.
    private void validatePermission(DistrictCover cover, Long userId) {
        if (cover.isTeamCover()) {
            if (!teamMemberRepository.existsByTeam_IdAndUser_Id(cover.getTeam().getId(), userId)) {
                throw new BusinessException(ErrorCode.DISTRICT_COVER_FORBIDDEN);
            }
        } else {
            if (!cover.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.DISTRICT_COVER_FORBIDDEN);
            }
        }
    }
}
