package com.kauniv.lightrip.global.ai.controller;

import com.kauniv.lightrip.global.ai.service.AiService;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI", description = "AI 초안 생성 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    // > AI 초안 생성 비즈니스 로직 담당.

    @Operation(summary = "AI 초안 생성", description = "이미지 URL로 블로그 초안과 카테고리 초기값을 생성합니다.")
    @PostMapping("/draft")
    public ResponseEntity<AiDraftResponse> generateDraft(@RequestParam String imageUrl) {
        // > 프론트가 S3 업로드 후 받은 CloudFront URL을 전달.
        // > 여권 등록 전에 호출해서 초안을 미리 보여주는 용도.

        AiDraftResponse response = aiService.generateDraft(imageUrl);
        return ResponseEntity.ok(response);
    }
}