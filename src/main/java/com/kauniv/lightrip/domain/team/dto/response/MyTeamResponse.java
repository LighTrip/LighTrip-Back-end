package com.kauniv.lightrip.domain.team.dto.response;

import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.team.entity.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 팀 조회 응답 (팀 ID + 팀장 + 팀원 목록)")
public record MyTeamResponse(

        @Schema(description = "팀 ID")
        Long teamId,

        @Schema(description = "팀 이름")
        String teamName,

        @Schema(description = "팀 코드")
        String teamCode,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "팀장 (role = LEADER)")
        TeamMemberResponse leader,

        @Schema(description = "전체 팀원 목록 (팀장 포함)")
        List<TeamMemberResponse> members
) {
    public static MyTeamResponse of(Team team, List<TeamMember> members) {
        List<TeamMemberResponse> memberResponses = members.stream()
                .map(TeamMemberResponse::from)
                .toList();

        TeamMemberResponse leader = members.stream()
                .filter(m -> m.getRole() == TeamMember.Role.LEADER)
                .map(TeamMemberResponse::from)
                .findFirst()
                .orElse(null);

        return new MyTeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getTeamCode(),
                team.getCreatedAt(),
                leader,
                memberResponses
        );
    }
}
