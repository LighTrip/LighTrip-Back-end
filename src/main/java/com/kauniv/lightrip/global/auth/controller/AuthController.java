package com.kauniv.lightrip.global.auth.controller;

import com.kauniv.lightrip.global.auth.dto.TokenResponse;
import com.kauniv.lightrip.global.auth.service.AuthService;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "토큰 재발급, 로그아웃, 회원탈퇴 기능을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰으로 새 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        String newAccessToken = authService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok(
                TokenResponse.builder()
                        .accessToken(newAccessToken)
                        .build()
        );
    }

    @Operation(summary = "로그아웃", description = "액세스 토큰을 블랙리스트에 등록하고 리프레시 토큰을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long userId,
            @RequestHeader("Authorization") String bearerToken) {
        if (!bearerToken.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String accessToken = bearerToken.substring(7);
        authService.logout(userId, accessToken);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원탈퇴", description = "유저 및 관련 인증 정보를 모두 삭제합니다.")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long userId,
            @RequestHeader("Authorization") String bearerToken) {
        if (!bearerToken.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String accessToken = bearerToken.substring(7);
        authService.withdraw(userId, accessToken);
        return ResponseEntity.ok().build();
    }
}