package com.kauniv.lightrip.domain.report.entity;

import com.kauniv.lightrip.domain.passport.entity.Passport;
import com.kauniv.lightrip.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 게시물(여권) 신고 1건.
 *
 * <p>(reporter, passport) 복합 유니크로 동일 사용자의 중복 신고를 차단한다.
 * 서로 다른 신고자 수가 임계값에 도달하면 {@link com.kauniv.lightrip.domain.report.service.ReportService}가
 * 해당 신고들을 {@link Status#AUTO_HIDDEN}으로 바꾸고 대상 여권을 숨김 처리한다.</p>
 */
@Entity
@Table(
        name = "passport_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_passport_report_reporter_passport",
                        columnNames = {"reporter_id", "passport_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PassportReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passport_report_id")
    private Long id;

    // > 신고자. 탈퇴 시 신고 이력도 함께 삭제(FK ON DELETE CASCADE).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // > 신고 대상 여권. 여권 삭제 시 신고 이력도 함께 삭제.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passport_id", nullable = false)
    private Passport passport;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private Reason reason;

    // > reason = ETC 일 때만 채워지는 자유 입력. 그 외에는 null.
    @Column(name = "detail", length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // > 누적 신고로 대상 여권이 자동 숨김될 때 함께 전이. 운영 통계에서 "자동 처리" 구분용.
    public void markAutoHidden() {
        this.status = Status.AUTO_HIDDEN;
    }

    /** 신고 사유. */
    public enum Reason {
        SPAM,        // 스팸/광고
        ABUSE,       // 욕설/비방/괴롭힘
        SEXUAL,      // 음란물/청소년 유해
        COPYRIGHT,   // 저작권/초상권 침해
        FALSE_INFO,  // 허위사실
        ETC          // 기타 (detail 필수)
    }

    /** 신고 처리 상태. */
    public enum Status {
        PENDING,      // 접수 (검토 대기)
        AUTO_HIDDEN,  // 누적 임계값 도달로 대상 자동 숨김
        RESOLVED,     // 운영자 검토 완료 — 조치함
        REJECTED      // 운영자 검토 완료 — 오신고
    }
}
