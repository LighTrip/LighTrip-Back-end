package com.kauniv.lightrip.domain.team.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationMessage {
    private Long userId;
    private String nickname;
    private Double latitude;
    private Double longitude;
    private String placeName;
    private Long passportId;
    private String timestamp; // 서버에서 ISO-8601로 덮어씀
}