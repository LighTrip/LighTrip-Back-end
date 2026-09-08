package com.kauniv.lightrip.domain.report.dto.response;

import com.kauniv.lightrip.domain.report.entity.PassportReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "게시물 신고 접수 결과")
public record PassportReportResponse(

        @Schema(description = "신고 ID")
        Long reportId,

        @Schema(description = "신고한 게시물 ID")
        Long passportId,

        @Schema(description = "신고 사유")
        PassportReport.Reason reason,

        @Schema(description = "신고 처리 상태", example = "PENDING")
        PassportReport.Status status,

        @Schema(description = "이 신고로 게시물이 자동 숨김되었는지 여부")
        boolean passportHidden,

        @Schema(description = "접수 시각")
        LocalDateTime createdAt
) {
    public static PassportReportResponse of(PassportReport report, boolean passportHidden) {
        // > report.getStatus()는 서비스에서 in-memory 동기화되어 DB와 일치한다
        // > (자동 숨김 발생 시 AUTO_HIDDEN, 그 외 PENDING).
        return new PassportReportResponse(
                report.getId(),
                report.getPassport().getId(),
                report.getReason(),
                report.getStatus(),
                passportHidden,
                report.getCreatedAt()
        );
    }
}
