package com.kauniv.lightrip.domain.passport.service;

import com.kauniv.lightrip.domain.passport.dto.request.DistrictCoverImageRequest;
import com.kauniv.lightrip.domain.passport.dto.request.DistrictCoverTextColorRequest;
import com.kauniv.lightrip.domain.passport.entity.DistrictCover;
import com.kauniv.lightrip.domain.passport.repository.DistrictCoverRepository;
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

    @Transactional
    public void updateTextColor(Long userId, Long coverId, DistrictCoverTextColorRequest req) {
        DistrictCover cover = districtCoverRepository.findById(coverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_COVER_NOT_FOUND));

        if (!cover.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.DISTRICT_COVER_FORBIDDEN);
        }

        cover.changeTextColor(req.textColor());
    }

    @Transactional
    public void updateCoverImage(Long userId, Long coverId, DistrictCoverImageRequest req) {
        DistrictCover cover = districtCoverRepository.findById(coverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_COVER_NOT_FOUND));

        if (!cover.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.DISTRICT_COVER_FORBIDDEN);
        }

        cover.changeCoverImage(req.imageUrl());
    }
}
