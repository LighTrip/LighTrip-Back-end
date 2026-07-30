package com.kauniv.lightrip.global.ai;

import com.kauniv.lightrip.global.ai.config.OpenAiProperties;
import com.kauniv.lightrip.global.ai.dto.ChatCompletionRequest;
import com.kauniv.lightrip.global.ai.dto.ChatCompletionResponse;
import com.kauniv.lightrip.global.ai.dto.EmbeddingRequest;
import com.kauniv.lightrip.global.ai.dto.EmbeddingResponse;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient openAiRestClient;
    // > 필드명 = 빈 이름(openAiRestClient)으로 매칭 — RestClient 빈이 2개라 이름으로 구분.
    // > 인증 헤더는 OpenAiConfig에서 프리셋 — 이 클래스는 API Key를 직접 다루지 않는다.

    private final OpenAiProperties openAiProperties;

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String EMBEDDINGS_PATH = "/embeddings";

    private static final int EMBEDDING_DIMENSIONS = 1536;
    // > passport.embedding 컬럼(vector(1536))과 일치해야 함. V8 마이그레이션 참조.

    private static final double DRAFT_TEMPERATURE = 0.7;
    // > 담백한 톤을 유지하되 매번 같은 문장이 나오지 않을 정도.

    /**
     * Chat Completions 호출. 모델이 반환한 본문(JSON 문자열)을 그대로 돌려준다.
     * 실패 시 BusinessException — 초안 생성은 사용자에게 결과를 보장해야 하므로 조용히 넘기지 않는다.
     */
    public String chat(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = ChatCompletionRequest.jsonMode(
                openAiProperties.model(), systemPrompt, userPrompt, DRAFT_TEMPERATURE);

        ChatCompletionResponse response = callWithRetry(
                () -> openAiRestClient.post()
                        .uri(CHAT_COMPLETIONS_PATH)
                        .body(request)
                        .retrieve()
                        .body(ChatCompletionResponse.class),
                CHAT_COMPLETIONS_PATH);

        String content = (response == null) ? null : response.firstContent();
        if (content == null || content.isBlank()) {
            log.error("OpenAI {} 응답에 본문이 없습니다.", CHAT_COMPLETIONS_PATH);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
        return content;
    }

    /**
     * 임베딩 생성. 실패해도 예외를 던지지 않고 null을 반환한다.
     * 임베딩은 여권 저장 후 부가 작업이자 RAG 참고자료용이라, 실패가 본 기능을 막으면 안 됨.
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        EmbeddingRequest request = EmbeddingRequest.of(
                openAiProperties.embeddingModel(), text, EMBEDDING_DIMENSIONS);

        try {
            EmbeddingResponse response = callWithRetry(
                    () -> openAiRestClient.post()
                            .uri(EMBEDDINGS_PATH)
                            .body(request)
                            .retrieve()
                            .body(EmbeddingResponse.class),
                    EMBEDDINGS_PATH);

            float[] result = (response != null) ? response.firstEmbedding() : new float[0];
            return result.length > 0 ? result : null;
        } catch (BusinessException e) {
            log.warn("OpenAI 임베딩 생성 실패 (textLength={}): {}", text.length(), e.getErrorCode().getCode());
            return null;
        }
    }

    // ========== 공통 호출/재시도 ==========

    // > 5xx·타임아웃만 1회 재시도. 4xx는 재시도해도 같은 결과라 즉시 변환한다.
    private <T> T callWithRetry(Supplier<T> call, String path) {
        try {
            return call.get();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("OpenAI {} 레이트리밋 초과(429)", path);
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("OpenAI {} 인증 실패(401) — OPENAI_API_KEY 환경변수를 확인하세요.", path);
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        } catch (HttpClientErrorException e) {
            log.error("OpenAI {} 요청 실패: {}", path, describe(e));
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("OpenAI {} 일시 오류({}) — 1회 재시도합니다.", path, describe(e));
            return retryOnce(call, path);
        }
    }

    private <T> T retryOnce(Supplier<T> call, String path) {
        try {
            return call.get();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("OpenAI {} 재시도 중 레이트리밋 초과(429)", path);
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            log.error("OpenAI {} 재시도 실패: {}", path, describe(e));
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    // > 예외를 상태코드/타입 수준으로만 요약한다.
    // > 원문 메시지에는 응답 본문이 통째로 실릴 수 있어 로그에 그대로 남기지 않는다.
    private String describe(RestClientException e) {
        if (e instanceof HttpStatusCodeException statusException) {
            return String.valueOf(statusException.getStatusCode());
        }
        return e.getClass().getSimpleName();
    }
}
