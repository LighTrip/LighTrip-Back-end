package com.kauniv.lightrip.domain.report.service;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.report.dto.request.PassportReportRequest;
import com.kauniv.lightrip.domain.report.dto.response.PassportReportResponse;
import com.kauniv.lightrip.domain.report.entity.PassportReport;
import com.kauniv.lightrip.domain.report.repository.PassportReportRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시물 신고 처리.
 *
 * <p>서로 다른 신고자 수가 임계값({@code ugc.report.auto-hide-threshold}, 기본 3)에 도달하면
 * 대상 여권을 즉시 {@link com.kauniv.lightrip.global.enums.PassportStatus#HIDDEN}으로 전환한다.
 * 이것이 App Store Guideline 1.2 의 "신고 후 24시간 내 조치" 요건을 자동으로 충족한다.
 * 이후 운영자 검토(RESOLVED/REJECTED)는 사후에 이뤄진다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PassportReportRepository reportRepository;
    private final PassportRepository passportRepository;
    private final UserRepository userRepository;

    // > 자동 숨김 임계값. 운영 중 조정 필요하면 .env / application.properties 에서 override.
    @Value("${ugc.report.auto-hide-threshold:3}")
    private int autoHideThreshold;

    @Transactional
    public PassportReportResponse report(Long reporterId, Long passportId, PassportReportRequest request) {
        // > 기타(ETC) 사유는 상세 내용 필수 — 무의미한 신고 남발 방지.
        if (request.isEtcWithoutDetail()) {
            throw new BusinessException(ErrorCode.REPORT_DETAIL_REQUIRED);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        // > 본인 글은 신고 불가.
        if (passport.isOwnedBy(reporterId)) {
            throw new BusinessException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
        }

        // > 같은 사람이 같은 글을 두 번 신고 불가 (유니크 제약 + 사전 검증).
        if (reportRepository.existsByReporter_IdAndPassport_Id(reporterId, passportId)) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
        }

        PassportReport report = reportRepository.save(
                PassportReport.builder()
                        .reporter(reporter)
                        .passport(passport)
                        .reason(request.reason())
                        .detail(request.reason() == PassportReport.Reason.ETC ? request.detail() : null)
                        .build()
        );
        reportRepository.flush();
        // > 방금 INSERT를 DB에 반영 → 아래 countByPassport_Id에 이 신고가 포함되도록.

        long distinctReporters = reportRepository.countByPassport_Id(passportId);
        if (passport.isActive() && distinctReporters >= autoHideThreshold) {
            passport.hide();
            reportRepository.markAllAutoHiddenByPassportId(passportId);
            report.markAutoHidden(); // 방금 저장한 신고도 in-memory 동기화 → 커밋 시 벌크 결과와 일관되게 flush
            log.info("[UGC] passport {} auto-hidden by {} reports (threshold {})",
                    passportId, distinctReporters, autoHideThreshold);
        }

        // > 이번 요청으로 숨겨졌든, 이전 신고로 이미 숨겨진 상태였든 모두 true.
        boolean passportHidden = !passport.isActive();
        return PassportReportResponse.of(report, passportHidden);
    }
}
