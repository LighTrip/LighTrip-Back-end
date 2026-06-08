package com.kauniv.lightrip.global.ai;

import com.kauniv.lightrip.global.ai.dto.AiResponse;
import com.kauniv.lightrip.global.ai.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient restClient;
    // > RestClientConfig에서 Bean으로 등록된 RestClient 주입.

    @Value("${ai.server.url}")
    private String aiServerUrl;
    // > application.properties의 ai.server.url 값 주입.

    public AiResponse generate(String imageUrl, String text, String authorization) {
        // > CloudFront URL에서 이미지를 다운로드해서 FastAPI로 multipart 전송.
        // > text: 사용자가 입력한 메모 텍스트. 없으면 image만 전송.
        // > authorization: 사용자 JWT를 AI 서버로 그대로 전달.

        // 1. 이미지 다운로드
        byte[] imageBytes;
        try {
            ResponseEntity<byte[]> imageResponse = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .toEntity(byte[].class);
            // > cdn.lightrip.cloud URL로 이미지 byte[] 다운로드.
            imageBytes = imageResponse.getBody();
        } catch (Exception e) {
            log.warn("AI 서버: 이미지 다운로드 실패 - {} : {}", imageUrl, e.getMessage());
            return null;
        }

        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("AI 서버: 이미지 응답이 비어있음 - {}", imageUrl);
            return null;
        }

        // 2. 파일명 추출
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        // > URL 마지막 경로에서 파일명 추출. multipart Content-Disposition에 필요.

        // 3. multipart/form-data 구성
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        // > byte[]를 ByteArrayResource로 감싸야 multipart 전송 가능.

        if (text != null) {
            body.add("text", text);
        }
        // > 프론트가 전달한 메모 텍스트를 함께 전송. 없으면 image만 전송.

        // 4. FastAPI 호출
        try {
            return restClient.post()
                    .uri(aiServerUrl + "/pipeline/generate")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AiResponse.class);
            // > POST /pipeline/generate 호출 후 AiResponse로 역직렬화.
            // > AI 서버는 Authorization 헤더의 JWT를 검증.
        } catch (Exception e) {
            log.warn("AI /pipeline/generate 호출 실패: {}", e.getMessage());
            return null;
            // > FastAPI 500 등 오류 시 null 반환 → AiService에서 빈 초안으로 처리.
        }
    }

    public float[] getEmbedding(String text, String authorization) {
        // > 텍스트를 1536차원 벡터로 변환. pgvector 저장 및 RAG 검색에 사용.
        // > authorization: FastAPI JWT 검증에 필요. generate()와 동일하게 전달.
        // > [AI 팀 구현 후 추가] POST /get-embedding, body: {"text": text}, header: Authorization
        // >   EmbeddingResponse.embedding() 반환값을 float[]로 return.
        log.warn("AI /get-embedding stub — AI 팀 구현 대기 중 (textLength={})",
                text != null ? text.length() : 0);
        return null;
    }

    public AiResponse generateWithContext(String imageUrl, List<String> context, String authorization) {
        // > 이미지 + 과거 기록 context를 함께 전송해서 개인화된 초안 생성.
        // > authorization: FastAPI JWT 검증에 필요. generate()와 동일하게 전달.
        // > [AI 팀 구현 후 추가] POST /generate-blog, multipart: image + context list, header: Authorization
        // >   generate()의 이미지 다운로드 + multipart 구성 패턴 그대로 재사용.
        log.warn("AI /generate-blog stub — AI 팀 구현 대기 중 (contextSize={})",
                context != null ? context.size() : 0);
        return null;
    }
}
