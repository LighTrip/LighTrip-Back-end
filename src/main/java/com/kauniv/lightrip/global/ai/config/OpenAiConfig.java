package com.kauniv.lightrip.global.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfig {

    @Bean
    public RestClient openAiRestClient(OpenAiProperties properties) {
        // > 연결 타임아웃은 HttpClient, 응답 타임아웃은 RequestFactory에 설정한다.
        // > RestClient.create()는 타임아웃이 무제한이라 외부 API 호출에 그대로 쓰면 안 됨.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout().connect())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout().read());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                // > 인증 헤더를 빈에 프리셋 — 호출부에서 키를 다루지 않게 해서 로그 유출 경로를 차단.
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
