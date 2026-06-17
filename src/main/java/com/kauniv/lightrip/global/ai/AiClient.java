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
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient restClient;
    // > RestClientConfig에서 Bean으로 등록된 RestClient 주입.

    @Value("${ai.server.url}")
    private String aiServerUrl;
    // > application.properties의 ai.server.url 값 주입.

    @Value("${ai.server.api-key}")
    private String apiKey;
    // > application.properties의 ai.server.api-key 값 주입 (AI 서버 인증용).

    public AiResponse generate(String imageUrl, String text, String references, String authorization) {
        // > CloudFront URL에서 이미지를 다운로드해서 FastAPI로 multipart 전송.
        // > text: 사용자 메모. references: pgvector 유사 기록 (없으면 생략). authorization: JWT 전달.


        // 1. 이미지 다운로드
        byte[] imageBytes;
        try {
            ResponseEntity<byte[]> imageResponse = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .toEntity(byte[].class);
            // > cdn.lightrip.cloud URL로 이미지 byte[] 다운로드.
            imageBytes = imageResponse.getBody();
        } catch (RestClientException e) {
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
        if (references != null) {
            body.add("references", references);
        }
        // > text: 사용자 메모. references: pgvector 유사 기록. AI가 각각 별도 입력으로 활용.

        // 4. FastAPI 호출
        try {
            return restClient.post()
                    .uri(aiServerUrl + "/pipeline/generate")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-API-Key", apiKey) // > AI 서버 검증용 인증 키 추가
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AiResponse.class);
            // > POST /pipeline/generate 호출 후 AiResponse로 역직렬화.
            // > AI 서버는 Authorization 헤더의 JWT를 검증.
        } catch (RestClientException e) {
            log.warn("AI /pipeline/generate 호출 실패: {}", e.getMessage());
            return null;
            // > FastAPI 500 등 오류 시 null 반환 → AiService에서 빈 초안으로 처리.
        }
    }

    public float[] getEmbedding(String text, String authorization) {
        // > POST /get-embedding, body: {"texts": [text]} (배치 API — 단일 텍스트 1개 전송).
        // > 응답: {"dim": 1536, "embeddings": [[...]]} → firstEmbedding()으로 float[] 추출.
        // > authorization: FastAPI JWT 검증에 필요.
        try {
            EmbeddingResponse response = restClient.post()
                    .uri(aiServerUrl + "/get-embedding")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-API-Key", apiKey) // > AI 서버 검증용 인증 키 추가
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", List.of(text)))
                    .retrieve()
                    .body(EmbeddingResponse.class);
            float[] result = response != null ? response.firstEmbedding() : new float[0];
            return result.length > 0 ? result : null;
        } catch (RestClientException e) {
            log.warn("AI /get-embedding 호출 실패 (textLength={}): {}",
                    text != null ? text.length() : 0, e.getMessage());
            return null;
        }
    }

}