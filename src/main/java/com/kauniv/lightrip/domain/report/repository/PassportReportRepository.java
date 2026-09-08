package com.kauniv.lightrip.domain.report.repository;

import com.kauniv.lightrip.domain.report.entity.PassportReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassportReportRepository extends JpaRepository<PassportReport, Long> {

    // > 중복 신고 차단. (reporter, passport) 유니크 제약과 짝을 이뤄 사전 검증에 사용.
    boolean existsByReporter_IdAndPassport_Id(Long reporterId, Long passportId);

    // > 누적 신고자 수. (reporter, passport) 유니크라 행 수 = 서로 다른 신고자 수.
    // > 이 값이 임계값 이상이면 대상 여권을 자동 숨김.
    long countByPassport_Id(Long passportId);

    // > 자동 숨김 시 해당 여권의 PENDING 신고를 일괄 AUTO_HIDDEN으로 전이 (운영 통계에서 자동 처리분 구분).
    // > clearAutomatically 사용 안 함: 같은 트랜잭션에서 passport.hide()가 dirty 상태로 대기 중인데
    // > 컨텍스트를 clear하면 그 변경이 flush되지 않아 자동 숨김이 무효화됨.
    // > 방금 저장한 report는 save() 후 수정하지 않으므로 dirty check에 안 걸려 벌크 결과를 덮어쓰지 않음.
    @Modifying
    @Query("""
            UPDATE PassportReport r
               SET r.status = com.kauniv.lightrip.domain.report.entity.PassportReport.Status.AUTO_HIDDEN
             WHERE r.passport.id = :passportId
               AND r.status = com.kauniv.lightrip.domain.report.entity.PassportReport.Status.PENDING
            """)
    void markAllAutoHiddenByPassportId(@Param("passportId") Long passportId);

    // > 여권 하드 삭제 시 신고 이력 선삭제 (like/scrap과 동일 패턴 — 로컬 DDL 환경 FK 대응).
    @Modifying
    @Query("DELETE FROM PassportReport r WHERE r.passport.id = :passportId")
    void deleteAllByPassportId(@Param("passportId") Long passportId);
}
