package com.kauniv.lightrip.global.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.global.ai.OpenAiClient;
import com.kauniv.lightrip.global.ai.dto.AiDraftRequest;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.prompt.DraftPrompt;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public AiDraftResponse generateDraft(Long userId, AiDraftRequest req) {
        String references = findReferences(userId, req);
        // > 과거 기록 기반 RAG. 실패하거나 기록이 없으면 null → 참고자료 없이 생성.

        String userPrompt = DraftPrompt.user(
                req.spaceName(), req.category(), req.districtCategory(),
                req.visitedAt(), req.keywords(), references);

        String content = openAiClient.chat(DraftPrompt.SYSTEM, userPrompt);
        // > 호출 실패 시 OpenAiClient가 BusinessException을 던진다.

        return new AiDraftResponse(parseDraft(content), req.category());
        // > category는 요청값을 그대로 반환 — 카테고리 자동 분류는 지원하지 않는다.
    }

    // > 요청 정보를 질의문으로 만들어 임베딩 → 본인의 유사 과거 기록을 참고자료로 뽑는다.
    // > 임베딩 실패는 초안 생성 자체를 막지 않는다 (참고자료 없이 진행).
    private String findReferences(Long userId, AiDraftRequest req) {
        if (userId == null) {
            return null;
        }

        float[] embedding = openAiClient.embed(toQueryText(req));
        if (embedding == null) {
            return null;
        }

        List<String> similarContents = passportRepository.findSimilarContents(
                userId, toVectorString(embedding), REFERENCE_LIMIT);
        // > 코사인 유사도 기준 상위 기록 텍스트 조회. user_id 필터로 본인 기록만 검색.

        return similarContents.isEmpty() ? null : String.join("\n---\n", similarContents);
    }

    // > 임베딩 검색용 질의문. 초안 본문이 아직 없으므로 입력 정보를 이어붙여 대신 사용한다.
    private String toQueryText(AiDraftRequest req) {
        List<String> parts = new ArrayList<>();
        parts.add(req.spaceName());
        parts.add(req.category().getDisplayName());
        parts.add(req.districtCategory().getDisplayName());
        if (req.keywords() != null && !req.keywords().isEmpty()) {
            parts.addAll(req.keywords());
        }
        return String.join(" ", parts);
    }

    // > 모델 응답 {"draft": "..."}에서 본문 추출.
    // > readTree: 모델이 필드를 더 붙여도 깨지지 않게 바인딩 대신 트리로 읽는다.
    private String parseDraft(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            String draft = node.path("draft").asText(null);

            if (draft == null || draft.isBlank()) {
                log.error("AI 응답에 draft 필드가 없습니다: {}", truncate(content));
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
            }
            return draft.trim();
        } catch (JsonProcessingException e) {
            log.error("AI 응답 JSON 파싱 실패: {}", truncate(content));
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
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
