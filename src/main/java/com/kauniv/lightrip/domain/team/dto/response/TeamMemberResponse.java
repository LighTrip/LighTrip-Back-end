package com.kauniv.lightrip.domain.team.dto.response;

import com.kauniv.lightrip.domain.team.entity.TeamMember;

public record TeamMemberResponse(
        Long userId,
        String nickname,
        String profileImg,
        TeamMember.Role role
) {
    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImg(),
                member.getRole()
        );
    }
}