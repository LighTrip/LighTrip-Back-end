package com.kauniv.lightrip.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(

        @JsonProperty("model")
        String model,

        @JsonProperty("messages")
        List<Message> messages,

        @JsonProperty("response_format")
        ResponseFormat responseFormat,
        // > {"type":"json_object"} 지정 시 모델이 유효한 JSON만 반환하도록 강제된다.
        // > 단, 프롬프트에도 JSON으로 답하라는 지시가 함께 있어야 API가 요청을 거부하지 않는다.

        @JsonProperty("temperature")
        Double temperature
) {

    public record Message(
            @JsonProperty("role")
            String role,
            // > system / user / assistant.

            @JsonProperty("content")
            String content
    ) {}

    public record ResponseFormat(
            @JsonProperty("type")
            String type
    ) {}

    // > JSON 강제 모드로 system + user 2턴 요청을 만드는 팩토리.
    public static ChatCompletionRequest jsonMode(
            String model, String systemPrompt, String userPrompt, double temperature) {
        return new ChatCompletionRequest(
                model,
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                new ResponseFormat("json_object"),
                temperature
        );
    }
}
