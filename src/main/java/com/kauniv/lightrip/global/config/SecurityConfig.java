package com.kauniv.lightrip.global.config;

import com.kauniv.lightrip.global.jwt.JwtFilter;
import com.kauniv.lightrip.global.jwt.JwtProvider;
import com.kauniv.lightrip.global.oauth.CustomOAuth2UserService;
import com.kauniv.lightrip.global.oauth.OAuth2SuccessHandler;
import com.kauniv.lightrip.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Bean
    @SuppressWarnings("java:S4502") // JWT stateless 인증 구조상 CSRF 공격 불가능
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/oauth2/**", "/login/**", "/auth/refresh", "/auth/apple/login",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**",
                                "/actuator/**", "/ws/**",
                                "/dev/**"   // dev 프로파일 전용 — @Profile("dev") 로 prod에서는 Bean 비활성화
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(
                        new JwtFilter(jwtProvider, redisService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}