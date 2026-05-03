package com.kauniv.lightrip.domain.like.controller;

import com.kauniv.lightrip.domain.like.dto.response.LikeListResponse;
import com.kauniv.lightrip.domain.like.dto.response.LikeResponse;
import com.kauniv.lightrip.domain.like.service.LikeService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like", description = "여권 좋아요 API")
@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "좋아요 등록",
            description = "여권에 좋아요를 등록합니다. 본인 여권도 좋아요 가능. 중복 좋아요 불가.")
    @PostMapping("/api/v1/passports/{passportId}/likes")
    public ApiResponse<LikeResponse> create(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        return ApiResponse.success("좋아요가 등록되었습니다.", likeService.create(userId, passportId));
    }

    @Operation(summary = "좋아요 취소",
            description = "여권 좋아요를 취소합니다.")
    @DeleteMapping("/api/v1/passports/{passportId}/likes")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        likeService.delete(userId, passportId);
        return ApiResponse.success("좋아요가 취소되었습니다.", null);
    }

    @Operation(summary = "내 좋아요 목록 조회",
            description = "내가 좋아요한 여권 목록을 커서 기반으로 조회합니다.")
    @GetMapping("/api/v1/passports/likes/me")
    public ApiResponse<CursorResponse<LikeListResponse>> getMyLikes(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "커서 (마지막 좋아요 ID, 첫 요청 시 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(likeService.getMyLikes(userId, cursor, size));
    }
}