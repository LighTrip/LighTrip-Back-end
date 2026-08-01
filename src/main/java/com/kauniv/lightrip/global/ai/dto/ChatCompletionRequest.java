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
            Object content
            // > 텍스트만이면 String, 이미지가 붙으면 ContentPart 배열.
            // > OpenAI가 두 형태를 모두 받으므로 타입을 Object로 두고 팩토리에서 결정한다.
    ) {

        public static Message text(String role, String text) {
            return new Message(role, text);
        }

        // > 텍스트 + 이미지 1장을 배열 형태 content로 묶는다.
        public static Message withImage(String role, String text, String imageUrl) {
            return new Message(role, List.of(
                    new TextPart("text", text),
                    new ImagePart("image_url", new ImageUrl(imageUrl, IMAGE_DETAIL))
            ));
        }
    }

    public record TextPart(
            @JsonProperty("type")
            String type,

            @JsonProperty("text")
            String text
    ) {}

    public record ImagePart(
            @JsonProperty("type")
            String type,

            @JsonProperty("image_url")
            ImageUrl imageUrl
    ) {}

    public record ImageUrl(
            @JsonProperty("url")
            String url,
            // > 공개 접근 가능한 URL이어야 한다. OpenAI 서버가 직접 받아간다.

            @JsonProperty("detail")
            String detail
    ) {}

    // > low: 해상도와 무관하게 이미지당 고정 토큰만 사용한다.
    // > 초안은 사진의 분위기·소재만 알면 충분해서 high로 올릴 이유가 없다 (토큰이 해상도에 비례해 늘어남).
    private static final String IMAGE_DETAIL = "low";

    public record ResponseFormat(
            @JsonProperty("type")
            String type
    ) {}

    // > JSON 강제 모드로 system + user 2턴 요청을 만드는 팩토리.
    // > imageUrl이 null이면 텍스트만으로 요청한다.
    public static ChatCompletionRequest jsonMode(
            String model, String systemPrompt, String userPrompt, String imageUrl, double temperature) {
        Message userMessage = (imageUrl == null || imageUrl.isBlank())
                ? Message.text("user", userPrompt)
                : Message.withImage("user", userPrompt, imageUrl);

        return new ChatCompletionRequest(
                model,
                List.of(Message.text("system", systemPrompt), userMessage),
                new ResponseFormat("json_object"),
                temperature
        );
    }
}
