package com.kauniv.lightrip.global.ai;

import com.kauniv.lightrip.global.ai.dto.AiResponse;
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
        // > authorization: 사용자 요청의 Authorization 헤더("Bearer ...")를 AI 서버로 그대로 전달.

        // 1. 이미지 다운로드
        ResponseEntity<byte[]> imageResponse = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .toEntity(byte[].class);
        // > cdn.lightrip.cloud URL로 이미지 byte[] 다운로드.

        byte[] imageBytes = imageResponse.getBody();
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("AI 서버: 이미지 다운로드 실패 - {}", imageUrl);
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
        // > 프론트가 전달한 설명 텍스트를 함께 전송. 없으면 image만 전송.

        // 4. FastAPI 호출
        return restClient.post()
                .uri(aiServerUrl + "/pipeline/generate")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AiResponse.class);
        // > POST /pipeline/generate 호출 후 AiResponse로 역직렬화.
        // > AI 서버는 다른 API와 동일하게 Authorization 헤더의 JWT를 검증.
    }
}