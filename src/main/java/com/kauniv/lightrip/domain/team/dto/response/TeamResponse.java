package com.kauniv.lightrip.domain.team.dto.response;

import com.kauniv.lightrip.domain.team.entity.Team;

import java.time.LocalDateTime;

public record TeamResponse(
        Long teamId,
        String teamName,
        String teamCode,
        LocalDateTime createdAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getTeamCode(),
                team.getCreatedAt()
        );
    }
}