package com.kauniv.lightrip.global.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.global.ai.OpenAiClient;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.prompt.DraftPrompt;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import com.kauniv.lightrip.global.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final OpenAiClient openAiClient;
    // > OpenAI 호출 담당 클라이언트.

    private final PassportRepository passportRepository;
    // > 유사 기록 검색(findSimilarContents) + 임베딩 저장(updateEmbedding)에 사용.

    private final ObjectMapper objectMapper;
    // > 모델이 반환한 JSON 문자열 파싱용.

    private static final int REFERENCE_LIMIT = 3;
    // > 프롬프트에 넣을 과거 기록 개수. 늘리면 토큰 비용과 응답 지연이 함께 늘어난다.

    private static final int PARSE_FAIL_LOG_LIMIT = 200;

    // ========== 초안 생성 ==========

    /**
     * 사진(+메모)으로 기록 초안과 카테고리를 생성한다.
     *
     * @param imageUrl 방문 사진 URL. OpenAI 서버가 직접 받아가므로 공개 접근 가능해야 한다.
     * @param text     사용자가 입력한 메모. 있으면 과거 기록 RAG 경로를 탄다.
     * @param userId   본인 기록만 검색하기 위한 식별자.
     */
    public AiDraftResponse generateDraft(String imageUrl, String text, Long userId) {
        String references = findReferences(text, userId);
        // > 과거 기록 기반 RAG. 메모가 없거나 조회에 실패하면 null → 참고자료 없이 생성.

        String content = openAiClient.chat(DraftPrompt.SYSTEM, DraftPrompt.user(text, references), imageUrl);
        // > 호출 실패 시 OpenAiClient가 BusinessException을 던진다.

        return parseDraft(content);
    }

    // > 메모를 임베딩해서 본인의 유사 과거 기록을 참고자료로 뽑는다.
    // > 메모가 없으면 검색할 질의문이 없으므로 RAG 자체를 건너뛴다 (구 API와 동일한 조건).
    private String findReferences(String text, Long userId) {
        if (userId == null || text == null || text.isBlank()) {
            return null;
        }

        float[] embedding = openAiClient.embed(text);
        if (embedding == null) {
            return null;
        }

        try {
            List<String> similarContents = passportRepository.findSimilarContents(
                    userId, toVectorString(embedding), REFERENCE_LIMIT);
            // > 코사인 유사도 기준 상위 기록 텍스트 조회. user_id 필터로 본인 기록만 검색.

            return similarContents.isEmpty() ? null : String.join("\n---\n", similarContents);
        } catch (DataAccessException e) {
            // > 참고자료 조회는 초안 품질을 높이는 부가 단계다. 여기서 실패해도 초안은 나와야 한다.
            // > pgvector 미설치·embedding 컬럼 부재(마이그레이션 누락) 같은 환경 문제로 500이 나가면 안 됨.
            log.warn("유사 기록 조회 실패 — 참고자료 없이 초안을 생성합니다 (userId={}): {}",
                    userId, e.getMostSpecificCause().getMessage());
            return null;
        }
    }

    // > 모델 응답 {"draft": "...", "category": "CAFE"} 파싱.
    // > readTree: 모델이 필드를 더 붙여도 깨지지 않게 바인딩 대신 트리로 읽는다.
    private AiDraftResponse parseDraft(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            String draft = node.path("draft").asText(null);

            if (draft == null || draft.isBlank()) {
                log.error("AI 응답에 draft 필드가 없습니다: {}", truncate(content));
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
            }
            return new AiDraftResponse(draft.trim(), parseCategory(node.path("category").asText(null)));
        } catch (JsonProcessingException e) {
            log.error("AI 응답 JSON 파싱 실패: {}", truncate(content));
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    private Category parseCategory(String categoryStr) {
        // > AI 반환값을 Category enum으로 안전하게 변환.
        // > 영문 enum 이름(예: "CAFE")을 요구하지만 한글 라벨("카페")로 답할 수도 있어 둘 다 허용.
        // > 매핑 실패 시 ETC로 폴백 — 카테고리 하나 때문에 초안 전체를 버리지 않는다.
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

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() <= PARSE_FAIL_LOG_LIMIT
                ? value
                : value.substring(0, PARSE_FAIL_LOG_LIMIT) + "...(생략)";
    }

    // ========== 임베딩 저장 ==========
    @Async
    @Transactional
    public void saveEmbeddingAsync(Long passportId, String content) {
        // > 여권 저장 후 비동기로 호출. content를 임베딩해서 pgvector에 저장.
        // > @Async: 여권 저장 API 응답 지연 방지 — OpenAI 왕복 시간을 백그라운드로 넘긴다.
        // > 실패해도 여권 저장 자체에는 영향 없음 (try-catch로 격리).
        try {
            float[] embedding = openAiClient.embed(content);
            if (embedding == null) {
                log.warn("임베딩 저장 skip: 임베딩 생성 실패 (passportId={})", passportId);
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
}
