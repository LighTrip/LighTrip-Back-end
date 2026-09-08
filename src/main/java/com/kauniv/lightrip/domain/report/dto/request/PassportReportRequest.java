package com.kauniv.lightrip.domain.report.dto.request;

import com.kauniv.lightrip.domain.report.entity.PassportReport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 게시물 신고 요청.
 *
 * <p>{@code reason = ETC} 인 경우 {@code detail} 이 필수다(서비스에서 검증).</p>
 */
public record PassportReportRequest(

        @Schema(description = "신고 사유", example = "ABUSE",
                allowableValues = {"SPAM", "ABUSE", "SEXUAL", "COPYRIGHT", "FALSE_INFO", "ETC"})
        @NotNull(message = "신고 사유는 필수입니다.")
        PassportReport.Reason reason,

        @Schema(description = "상세 내용 (reason = ETC 일 때 필수, 최대 500자)", example = "부적절한 홍보 링크가 포함되어 있습니다.")
        @Size(max = 500, message = "상세 내용은 500자 이하로 입력해주세요.")
        String detail
) {
    public boolean isEtcWithoutDetail() {
        return reason == PassportReport.Reason.ETC
                && (detail == null || detail.isBlank());
    }
}
