package com.kauniv.lightrip.global.ai.controller;

import com.kauniv.lightrip.global.ai.service.AiService;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI", description = "AI 초안 생성 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    // > AI 초안 생성 비즈니스 로직 담당.

    @Operation(summary = "AI 초안 생성",
            description = "이미지 URL + 메모로 블로그 초안과 카테고리 초기값을 생성합니다. " +
                    "text 전달 시 과거 기록 기반 RAG 경로로 개인화된 초안을 생성하며, " +
                    "미전달 시 이미지 기반 기본 초안을 생성합니다.")
    @PostMapping("/draft")
    public ResponseEntity<AiDraftResponse> generateDraft(
            @RequestParam String imageUrl,
            @RequestParam(required = false) String text,
            // > 사용자가 입력한 메모 텍스트. RAG 경로 활성화 + FastAPI 전달에 사용. optional.
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            // > 사용자 JWT. FastAPI AI 서버 인증에 사용.
            @AuthenticationPrincipal Long userId
            // > JWT에서 추출한 userId. 본인 과거 기록만 검색하기 위해 사용.
    ) {
        AiDraftResponse response = aiService.generateDraft(imageUrl, text, authorization, userId);
        return ResponseEntity.ok(response);
    }
}
