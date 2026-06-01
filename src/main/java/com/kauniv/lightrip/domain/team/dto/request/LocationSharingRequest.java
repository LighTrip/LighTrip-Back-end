package com.kauniv.lightrip.domain.team.dto.request;

// PATCH /teams/{teamId}/live-locations/me 요청 DTO
// sharing: false → Redis 키 즉시 삭제 (지도에서 바로 제외)
// sharing: true  → 서버 처리 없음, 클라이언트가 WebSocket 전송 재개하면 자동 등록
public record LocationSharingRequest(boolean sharing) {}
