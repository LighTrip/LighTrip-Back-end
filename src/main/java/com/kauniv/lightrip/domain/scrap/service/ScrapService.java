package com.kauniv.lightrip.domain.scrap.service;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.domain.scrap.dto.response.ScrapResponse;
import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
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
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final PassportService passportService;

    // ========== 스크랩 등록 ==========
    @Transactional
    public ScrapResponse create(Long userId, Long passportId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 여권 존재 + visibility 접근 권한 검증
        Passport passport = passportService.getPassportWithReadCheck(userId, passportId);

        // 중복 스크랩 체크
        if (scrapRepository.existsByUser_IdAndPassport_Id(userId, passportId)) {
            throw new BusinessException(ErrorCode.SCRAP_DUPLICATE);
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .passport(passport)
                .build();

        return ScrapResponse.from(scrapRepository.save(scrap));
    }

    // ========== 스크랩 취소 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Scrap scrap = scrapRepository.findByUser_IdAndPassport_Id(userId, passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.delete(scrap);
    }
}