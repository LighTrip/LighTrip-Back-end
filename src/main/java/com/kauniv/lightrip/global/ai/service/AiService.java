package com.kauniv.lightrip.global.ai.service;

import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.global.ai.AiClient;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.dto.AiResponse;
import com.kauniv.lightrip.global.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient aiClient;
    // > FastAPI 호출 담당 클라이언트.

    private final PassportRepository passportRepository;
    // > 유사 기록 검색(findSimilarContents) + 임베딩 저장(updateEmbedding)에 사용.

    public AiDraftResponse generateDraft(String imageUrl, String text, String authorization, Long userId) {
        // > RAG 경로: text와 userId가 모두 있을 때 과거 기록을 context로 주입.
        // > fallback: stub 반환값이 null이거나 text가 없으면 기존 이미지 기반 초안으로 폴백.

        if (text != null && !text.isBlank() && userId != null) {
            float[] embedding = aiClient.getEmbedding(text, authorization);
            // > 메모 텍스트를 벡터로 변환 — 유사 과거 기록 검색에 사용.
            // > authorization: FastAPI JWT 검증에 사용. generate()와 동일하게 전달.

            if (embedding != null) {
                List<String> similarContents = passportRepository.findSimilarContents(
                        userId, toVectorString(embedding), 3);
                // > 코사인 유사도 기준 상위 3개 과거 기록 텍스트 조회.

                if (!similarContents.isEmpty()) {
                    String references = String.join("\n---\n", similarContents);
                    // > 유사 기록 3개를 구분자로 연결 → FastAPI references 필드로 전달 (text와 별도 입력).
                    AiResponse aiResponse = aiClient.generate(imageUrl, text, references, authorization);

                    if (aiResponse != null) {
                        return new AiDraftResponse(aiResponse.draft(), parseCategory(aiResponse.category()));
                    }
                }
            }
        }

        // fallback: references 없이 이미지 + 텍스트만으로 초안 생성
        AiResponse aiResponse = aiClient.generate(imageUrl, text, null, authorization);
        if (aiResponse == null) {
            return new AiDraftResponse(null, null);
            // > AI 서버 장애 또는 stub 상태. 프론트에서 빈 값으로 처리.
        }
        return new AiDraftResponse(aiResponse.draft(), parseCategory(aiResponse.category()));
    }

    @Async
    @Transactional
    public void saveEmbeddingAsync(Long passportId, String content, String authorization) {
        // > 여권 저장 후 비동기로 호출. content를 임베딩해서 pgvector에 저장.
        // > @Async: 여권 저장 API 응답 지연 방지 — Gemma 처리 시간을 백그라운드에서 처리.
        // > authorization: FastAPI /get-embedding JWT 검증에 사용. HTTP 요청 스레드에서 받아서 전달.
        // > 실패해도 여권 저장 자체에는 영향 없음 (try-catch로 격리).
        try {
            float[] embedding = aiClient.getEmbedding(content, authorization);
            if (embedding == null) {
                log.warn("임베딩 저장 skip: /get-embedding stub 상태 (passportId={})", passportId);
                return;
            }
            passportRepository.updateEmbedding(passportId, toVectorString(embedding));
            log.info("임베딩 저장 완료 (passportId={})", passportId);
        } catch (RuntimeException e) {
            log.error("임베딩 저장 실패 (passportId={}): {}", passportId, e.getMessage());
        }
    }

    private String toVectorString(float[] vector) {
        // > float[] → "[0.1,-0.2,0.3,...]" 형식 변환.
        // > pgvector CAST(:embedding AS vector) 구문에서 파싱 가능한 포맷.
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }

    private Category parseCategory(String categoryStr) {
        // > AI 반환값을 Category enum으로 안전하게 변환.
        // > AI 서버는 한글 라벨(displayName, 예: "카페")을 반환하므로 displayName으로 매칭.
        // > 영문 enum 이름(예: "CAFE")도 허용. 매핑 실패 시 ETC로 폴백.
        if (categoryStr == null) {
            return Category.ETC;
        }

        String value = categoryStr.trim();
        for (Category category : Category.values()) {
            if (category.getDisplayName().equals(value) || category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }

        log.warn("AI 카테고리 매핑 실패: {} → ETC로 폴백", categoryStr);
        return Category.ETC;
    }
}
