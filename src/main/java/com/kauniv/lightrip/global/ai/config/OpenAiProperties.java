package com.kauniv.lightrip.global.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(

        String apiKey,
        // > 환경변수 OPENAI_API_KEY로 주입. 절대 로그·응답에 노출하지 않는다.

        String model,
        // > Chat Completions에 사용할 모델 (예: gpt-4o-mini).

        String embeddingModel,
        // > 임베딩 생성에 사용할 모델. pgvector 컬럼이 vector(1536)이므로 1536차원 모델이어야 한다.

        String baseUrl,
        // > OpenAI API 베이스 URL (https://api.openai.com/v1).

        Timeout timeout
) {

    public record Timeout(
            Duration connect,
            // > 연결 타임아웃. 기본 3초.

            Duration read
            // > 응답 타임아웃. 기본 15초.
    ) {}

    // > record 기본 toString은 apiKey를 그대로 출력 → 설정 로깅/디버깅 시 키 유출.
    // > 마스킹된 toString으로 오버라이드해서 어떤 경로로도 키가 로그에 남지 않게 한다.
    @Override
    public String toString() {
        return "OpenAiProperties{model=" + model
                + ", embeddingModel=" + embeddingModel
                + ", baseUrl=" + baseUrl
                + ", timeout=" + timeout
                + ", apiKey=****}";
    }
}
