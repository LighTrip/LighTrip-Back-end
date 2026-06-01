package com.kauniv.lightrip.domain.team.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kauniv.lightrip.domain.team.dto.request.LocationMessage;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class MapSyncController {

    // SimpMessagingTemplate: @SendTo 대신 사용 — 검증 후에만 브로드캐스트하기 위함
    // TeamMemberRepository: 팀 멤버 여부 검증
    // RedisTemplate: 현재 위치 저장 (TTL 5분, 오프라인 시 자동 만료)
    // ObjectMapper: LocationMessage → JSON 직렬화
    private final SimpMessagingTemplate messagingTemplate;
    private final TeamMemberRepository teamMemberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration LOCATION_TTL = Duration.ofMinutes(5);

    @MessageMapping("/team/{teamId}/location")
    public void syncLocation(
            @DestinationVariable Long teamId,
            LocationMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) throws JsonProcessingException {

        // StompHandler가 CONNECT 시 세션에 저장한 userId 사용
        // 클라이언트가 보낸 message.getUserId()는 무시 — 위변조 방지
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        // 해당 팀의 멤버인지 검증. 비멤버는 메시지 무시
        if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
            return;
        }

        // 서버 타임스탬프 + 인증된 userId로 교체한 검증 메시지 생성
        LocationMessage verified = new LocationMessage(
                userId,
                message.getNickname(),
                message.getLatitude(),
                message.getLongitude(),
                message.getPlaceName(),
                message.getPassportId(),
                Instant.now().toString()
        );

        // Redis 저장 key: live:team:{teamId}:{userId}
        // TTL 초과 시 자동 삭제 → GET /live-locations 응답에서 자동 제외
        String key = "live:team:" + teamId + ":" + userId;
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(verified), LOCATION_TTL);

        // 같은 팀 구독자 전원에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/team/" + teamId, verified);
    }
}
