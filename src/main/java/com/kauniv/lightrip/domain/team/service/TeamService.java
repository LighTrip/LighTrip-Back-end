package com.kauniv.lightrip.domain.team.service;

import com.kauniv.lightrip.domain.team.dto.request.TeamCreateRequest;
import com.kauniv.lightrip.domain.team.dto.request.TeamJoinRequest;
import com.kauniv.lightrip.domain.team.dto.response.TeamMapResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamMemberResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamResponse;
import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.team.entity.TeamMember;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import com.kauniv.lightrip.domain.team.repository.TeamRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final PassportRepository passportRepository;

    @Transactional
    public TeamResponse create(Long userId, TeamCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String teamCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Team team = Team.builder()
                .teamName(req.teamName())
                .teamCode(teamCode)
                .build();
        teamRepository.save(team);

        TeamMember leader = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMember.Role.LEADER)
                .build();
        teamMemberRepository.save(leader);

        return TeamResponse.from(team);
    }

    public TeamResponse searchByCode(String code) {
        Team team = teamRepository.findByTeamCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_INVALID_CODE));
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse join(Long userId, TeamJoinRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Team team = teamRepository.findByTeamCode(req.teamCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_INVALID_CODE));

        if (teamMemberRepository.existsByTeam_IdAndUser_Id(team.getId(), userId)) {
            throw new BusinessException(ErrorCode.TEAM_ALREADY_JOINED);
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMember.Role.MEMBER)
                .build();
        teamMemberRepository.save(member);

        return TeamResponse.from(team);
    }

    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        return TeamResponse.from(team);
    }

    public List<TeamMemberResponse> getMembers(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        return teamMemberRepository.findAllByTeam_Id(teamId).stream()
                .map(TeamMemberResponse::from)
                .collect(Collectors.toList());
    }

    public List<TeamMapResponse> getTeamMap(Long userId, Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }

        return passportRepository.findAllByTeam_Id(teamId).stream()
                .map(p -> new TeamMapResponse(
                        p.getUser().getId(),
                        p.getUser().getNickname(),
                        p.getLatitude(),
                        p.getLongitude()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void leave(Long userId, Long teamId) {
        TeamMember member = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_MEMBER));

        if (member.getRole() == TeamMember.Role.LEADER) {
            throw new BusinessException(ErrorCode.TEAM_LEADER_CANNOT_LEAVE);
        }

        teamMemberRepository.delete(member);
    }
}