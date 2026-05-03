package com.kauniv.lightrip.domain.scrap.service;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.domain.scrap.dto.response.ScrapListResponse;
import com.kauniv.lightrip.domain.scrap.dto.response.ScrapResponse;
import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import com.kauniv.lightrip.domain.scrap.repository.ScrapRepository;
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
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final PassportService passportService;

    // ========== 스크랩 등록 ==========
    @Transactional
    public ScrapResponse create(Long userId, Long passportId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Passport passport = passportService.getPassportWithReadCheck(userId, passportId);

        if (scrapRepository.existsByUser_IdAndPassport_Id(userId, passportId)) {
            throw new BusinessException(ErrorCode.SCRAP_DUPLICATE);
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .passport(passport)
                .build();

        passport.increaseScrapCount();

        return ScrapResponse.from(scrapRepository.save(scrap));
    }

    // ========== 스크랩 취소 ==========
    @Transactional
    public void delete(Long userId, Long passportId) {
        Scrap scrap = scrapRepository.findByUser_IdAndPassport_Id(userId, passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        scrap.getPassport().decreaseScrapCount();

        scrapRepository.delete(scrap);
    }

    // ========== 내 스크랩 목록 조회 (커서 기반) ==========
    public CursorResponse<ScrapListResponse> getMyScraps(Long userId, Long cursor, int size) {
        List<Scrap> scraps = (cursor == null)
                ? scrapRepository.findByUserIdFirst(userId, PageRequest.of(0, size + 1))
                : scrapRepository.findByUserIdAfterCursor(userId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = scraps.size() > size;

        if (hasNext) {
            scraps = scraps.subList(0, size);
        }

        List<ScrapListResponse> content = scraps.stream()
                .map(ScrapListResponse::from)
                .toList();

        Long nextCursor = hasNext ? scraps.get(scraps.size() - 1).getId() : null;

        return CursorResponse.of(content, hasNext, nextCursor);
    }
}