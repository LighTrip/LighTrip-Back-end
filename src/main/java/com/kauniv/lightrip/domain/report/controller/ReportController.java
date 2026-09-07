package com.kauniv.lightrip.domain.report.controller;

import com.kauniv.lightrip.domain.report.dto.request.PassportReportRequest;
import com.kauniv.lightrip.domain.report.dto.response.PassportReportResponse;
import com.kauniv.lightrip.domain.report.service.ReportService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신고 API", description = "게시물(여권) 신고 기능. UGC 콘텐츠 정책 대응.")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "게시물 신고",
            description = """
                    다른 사용자의 여권을 신고합니다.

                    - 본인 게시물 신고 불가 (400 `RP001`)
                    - 같은 게시물 중복 신고 불가 (409 `RP002`)
                    - `reason = ETC` 인 경우 `detail` 필수 (400 `RP003`)
                    - 서로 다른 신고자가 임계값 이상 신고하면 게시물이 자동 숨김되며,
                      응답의 `passportHidden = true` 로 표시됩니다.
                    """)
    @PostMapping("/api/v1/passports/{passportId}/reports")
    public ResponseEntity<ApiResponse<PassportReportResponse>> report(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "신고할 여권 ID") @PathVariable Long passportId,
            @Valid @RequestBody PassportReportRequest request
    ) {
        PassportReportResponse response = reportService.report(userId, passportId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다.", response));
    }
}
