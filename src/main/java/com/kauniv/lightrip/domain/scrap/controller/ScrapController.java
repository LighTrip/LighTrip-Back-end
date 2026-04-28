package com.kauniv.lightrip.domain.scrap.controller;

import com.kauniv.lightrip.domain.scrap.dto.response.ScrapListResponse;
import com.kauniv.lightrip.domain.scrap.dto.response.ScrapResponse;
import com.kauniv.lightrip.domain.scrap.service.ScrapService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Scrap", description = "여권 스크랩 API")
@RestController
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @Operation(summary = "스크랩 등록",
            description = "여권을 스크랩합니다. 본인 여권도 스크랩 가능. 중복 스크랩 불가.")
    @PostMapping("/api/v1/passports/{passportId}/scraps")
    public ApiResponse<ScrapResponse> create(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        return ApiResponse.success("스크랩이 등록되었습니다.", scrapService.create(userId, passportId));
    }

    @Operation(summary = "스크랩 취소",
            description = "여권 스크랩을 취소합니다.")
    @DeleteMapping("/api/v1/passports/{passportId}/scraps")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        scrapService.delete(userId, passportId);
        return ApiResponse.success("스크랩이 취소되었습니다.", null);
    }

    @Operation(summary = "내 스크랩 목록 조회",
            description = "내가 스크랩한 여권 목록을 커서 기반으로 조회합니다.")
    @GetMapping("/api/v1/passports/scraps/me")
    public ApiResponse<CursorResponse<ScrapListResponse>> getMyScraps(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "커서 (마지막 스크랩 ID, 첫 요청 시 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(scrapService.getMyScraps(userId, cursor, size));
    }
}