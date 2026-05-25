package com.kauniv.lightrip.domain.passport.controller;

import com.kauniv.lightrip.domain.passport.dto.response.LightResponse;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Map", description = "지도 불빛 API")
@RestController
@RequestMapping("/api/v1/lights")
@RequiredArgsConstructor
public class LightController {

    private final PassportService passportService;

    @Operation(summary = "내 불빛 조회",
            description = "지도 화면에 표시할 여권 좌표를 Bounding Box 범위 내에서 조회합니다. " +
                    "teamId 미지정 시 본인 여권(모든 visibility) 조회, " +
                    "teamId 지정 시 해당 팀 여권(visibility 무시) 조회 — 팀 멤버만 허용.")
    @GetMapping("/me")
    public ApiResponse<List<LightResponse>> getMyLights(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "최소 위도 (좌하단)") @RequestParam BigDecimal minLat,
            @Parameter(description = "최대 위도 (우상단)") @RequestParam BigDecimal maxLat,
            @Parameter(description = "최소 경도 (좌하단)") @RequestParam BigDecimal minLng,
            @Parameter(description = "최대 경도 (우상단)") @RequestParam BigDecimal maxLng,
            @Parameter(description = "팀 ID (선택) — 지정 시 해당 팀 여권 조회") @RequestParam(required = false) Long teamId
    ) {
        return ApiResponse.success(
                passportService.getMyLights(userId, minLat, maxLat, minLng, maxLng, teamId)
        );
    }

    @Operation(summary = "특정 사용자 불빛 조회",
            description = "지정된 사용자가 작성한 여권 좌표를 Bounding Box 범위 내에서 조회합니다. " +
                    "PUBLIC 노출 + 친구 관계인 경우 FRIENDS_ONLY 포함.")
    @GetMapping("/{userId}")
    public ApiResponse<List<LightResponse>> getUserLights(
            @AuthenticationPrincipal Long viewerId,
            @Parameter(description = "조회 대상 사용자 ID") @PathVariable Long userId,
            @Parameter(description = "최소 위도 (좌하단)") @RequestParam BigDecimal minLat,
            @Parameter(description = "최대 위도 (우상단)") @RequestParam BigDecimal maxLat,
            @Parameter(description = "최소 경도 (좌하단)") @RequestParam BigDecimal minLng,
            @Parameter(description = "최대 경도 (우상단)") @RequestParam BigDecimal maxLng
    ) {
        return ApiResponse.success(
                passportService.getUserLights(viewerId, userId, minLat, maxLat, minLng, maxLng)
        );
    }
}
