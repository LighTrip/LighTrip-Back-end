package com.kauniv.lightrip.domain.passport.controller;

import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
import com.kauniv.lightrip.domain.passport.dto.response.PassportResponse;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Passport", description = "여권 API")
@RestController
@RequestMapping("/api/v1/passports")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;

    @Operation(summary = "여권 등록",
            description = "방문 기록을 여권으로 등록합니다. 이미지 1~5장 필수.")
    @PostMapping
    public ApiResponse<PassportResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PassportCreateRequest request
    ) {
        return ApiResponse.success("여권이 등록되었습니다.", passportService.create(userId, request));
    }

    @Operation(summary = "여권 수정",
            description = "여권 정보를 수정합니다. 위치/날짜 수정 불가. 개인=본인만, 팀=팀원 누구나.")
    @PatchMapping("/{passportId}")
    public ApiResponse<PassportResponse> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId,
            @Valid @RequestBody PassportUpdateRequest request
    ) {
        return ApiResponse.success("여권이 수정되었습니다.", passportService.update(userId, passportId, request));
    }

    @Operation(summary = "여권 삭제",
            description = "여권을 삭제합니다. 작성자만 삭제 가능하며, 연관 스크랩도 함께 삭제됩니다.")
    @DeleteMapping("/{passportId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        passportService.delete(userId, passportId);
        return ApiResponse.success("여권이 삭제되었습니다.", null);
    }
}