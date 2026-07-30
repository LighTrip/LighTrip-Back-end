package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// > ignoreUnknown: OpenAI 응답에는 id/object/created/usage 등 부가 필드가 많고 예고 없이 늘어난다.
// > 이 프로젝트의 ObjectMapper는 기본 설정이라 명시하지 않으면 미지의 필드에서 역직렬화가 실패한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(

        @JsonProperty("choices")
        List<Choice> choices
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("message")
            Message message,

            @JsonProperty("finish_reason")
            String finishReason
            // > "length"면 max_tokens에 걸려 잘린 응답 — JSON 파싱이 깨질 수 있다.
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("content")
            String content
            // > 모델이 생성한 본문. json_object 모드에서는 JSON 문자열이 들어있다.
    ) {}

    // > 첫 번째 choice의 본문 추출. 응답이 비어있으면 null.
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice first = choices.getFirst();
        return (first == null || first.message() == null) ? null : first.message().content();
    }
}
