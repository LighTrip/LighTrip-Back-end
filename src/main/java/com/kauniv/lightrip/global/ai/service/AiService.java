package com.kauniv.lightrip.global.ai.service;

import com.kauniv.lightrip.global.ai.AiClient;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.dto.AiResponse;
import com.kauniv.lightrip.global.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient aiClient;
    // > FastAPI 호출 담당 클라이언트.

    public AiDraftResponse generateDraft(String imageUrl, String text, String authorization) {
        // > 이미지 URL과 설명 텍스트로 AI 초안과 카테고리 초기값을 생성해서 반환.
        // > authorization: 사용자 JWT를 AI 서버로 전달하기 위한 Authorization 헤더값.

        AiResponse aiResponse = aiClient.generate(imageUrl, text, authorization);
        // > FastAPI /pipeline/generate 호출.

        if (aiResponse == null) {
            return new AiDraftResponse(null, null);
            // > AI 서버 장애 시 null 반환. 프론트에서 빈 값으로 처리.
        }

        Category category = parseCategory(aiResponse.category());
        // > FastAPI가 반환한 영문 문자열을 Category enum으로 변환.

        return new AiDraftResponse(aiResponse.draft(), category);
    }

    private Category parseCategory(String categoryStr) {
        // > AI 반환값을 Category enum으로 안전하게 변환.
        // > 매핑 실패 시 ETC로 폴백.
        try {
            return Category.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            log.warn("AI 카테고리 매핑 실패: {} → ETC로 폴백", categoryStr);
            return Category.ETC;
        }
    }
}