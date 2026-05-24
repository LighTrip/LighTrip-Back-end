package com.kauniv.lightrip.domain.user.controller;

import com.kauniv.lightrip.domain.user.dto.request.CompleteProfileRequest;
import com.kauniv.lightrip.domain.user.dto.request.UpdateProfileRequest;
import com.kauniv.lightrip.domain.user.dto.response.MyProfileResponse;
import com.kauniv.lightrip.domain.user.dto.response.PublicProfileResponse;
import com.kauniv.lightrip.domain.user.service.UserService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저 프로필 API", description = "내 프로필 조회/편집 및 공개 프로필 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 본인의 전체 프로필을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    @Operation(summary = "온보딩 프로필 설정", description = "카카오 회원가입 후 닉네임, 활동 지역, 한 줄 소개를 초기 설정합니다.")
    @PatchMapping("/me/onboarding")
    public ApiResponse<MyProfileResponse> completeOnboarding(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CompleteProfileRequest request) {
        return ApiResponse.success(userService.completeOnboarding(userId, request));
    }

    @Operation(summary = "내 프로필 편집", description = "닉네임과 프로필 이미지를 수정합니다.")
    @PutMapping("/me")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateMyProfile(userId, request));
    }

    @Operation(summary = "공개 프로필 조회", description = "다른 유저의 공개 프로필을 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<PublicProfileResponse> getPublicProfile(
            @PathVariable Long userId) {
        return ApiResponse.success(userService.getPublicProfile(userId));
    }
}