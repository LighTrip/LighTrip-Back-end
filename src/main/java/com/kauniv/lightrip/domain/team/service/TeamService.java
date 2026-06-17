package com.kauniv.lightrip.domain.team.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kauniv.lightrip.domain.team.dto.request.LocationMessage;
import com.kauniv.lightrip.domain.team.dto.request.TeamCreateRequest;
import com.kauniv.lightrip.domain.team.dto.request.TeamJoinRequest;
import com.kauniv.lightrip.domain.team.dto.response.LiveLocationResponse;
import com.kauniv.lightrip.domain.team.dto.response.MyTeamResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamMemberResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamResponse;
import com.kauniv.lightrip.domain.team.entity.Team;
import com.kauniv.lightrip.domain.team.entity.TeamMember;
import com.kauniv.lightrip.domain.team.repository.TeamMemberRepository;
import com.kauniv.lightrip.domain.team.repository.TeamRepository;
import com.kauniv.lightrip.domain.passport.repository.PassportRepository;
import com.kauniv.lightrip.domain.user.entity.User;
import com.kauniv.lightrip.domain.user.repository.UserRepository;
import com.kauniv.lightrip.global.common.exception.BusinessException;
import com.kauniv.lightrip.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

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

    public MyTeamResponse getMyTeam(Long userId) {
        TeamMember membership = teamMemberRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_JOINED));
        Team team = membership.getTeam();
        List<TeamMember> members = teamMemberRepository.findAllByTeam_Id(team.getId());
        return MyTeamResponse.of(team, members);
    }

    public List<TeamMemberResponse> getMembers(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        return teamMemberRepository.findAllByTeam_Id(teamId).stream()
                .map(TeamMemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void leave(Long userId, Long teamId) {
        TeamMember member = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_MEMBER));

        if (member.getRole() == TeamMember.Role.LEADER) {
            // > LEADER 탈퇴 = 팀 해산. passport.team_id FK 제약 때문에 먼저 null 처리 후 삭제.
            passportRepository.clearTeamReference(teamId);
            teamMemberRepository.deleteAllByTeam_Id(teamId);
            teamRepository.delete(member.getTeam());
            return;
        }

        teamMemberRepository.delete(member);
    }

    // > Redis에서 현재 온라인 팀원 위치 조회.
    // > TTL 5분 초과(오프라인) 유저는 키가 없으므로 null → filter로 제거.
    // > MapSyncController와 Redis key 구조 공유 (key 패턴: "live:team:[teamId]:[userId]")
    public List<LiveLocationResponse> getLiveLocations(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        return teamMemberRepository.findAllByTeam_Id(teamId).stream()
                .map(member -> {
                    String key = "live:team:" + teamId + ":" + member.getUser().getId();
                    String json = redisTemplate.opsForValue().get(key);
                    if (json == null) return null;
                    try {
                        LocationMessage msg = objectMapper.readValue(json, LocationMessage.class);
                        return new LiveLocationResponse(
                                msg.getUserId(), msg.getNickname(),
                                msg.getLatitude(), msg.getLongitude(),
                                msg.getPlaceName(), msg.getPassportId(),
                                msg.getTimestamp()
                        );
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void updateLocationSharing(Long userId, Long teamId, boolean sharing) {
        // > sharing=false 시 Redis 키 즉시 삭제 → GET /live-locations 응답에서 즉시 제외.
        // > sharing=true 시 아무 작업 없음 — 실제 위치 데이터는 WebSocket 메시지 수신 시 저장됨.
        if (!teamRepository.existsById(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        if (!teamMemberRepository.existsByTeam_IdAndUser_Id(teamId, userId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }
        if (!sharing) {
            String key = "live:team:" + teamId + ":" + userId;
            redisTemplate.delete(key);
        }
    }
}