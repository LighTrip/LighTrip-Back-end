package com.kauniv.lightrip.domain.team.dto.response;

// GET /teams/{teamId}/live-locations REST 응답 DTO
// WebSocket 브로드캐스트 메시지와 동일한 구조 — 지도 최초 진입 시 REST로 가져오고
// 이후 업데이트는 /topic/team/{teamId} 구독으로 수신
public record LiveLocationResponse(
        Long userId,
        String nickname,
        Double latitude,
        Double longitude,
        String placeName,
        Long passportId,
        String timestamp
) {}
