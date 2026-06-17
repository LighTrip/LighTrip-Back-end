package com.kauniv.lightrip.domain.team.controller;

import com.kauniv.lightrip.domain.team.dto.request.LocationSharingRequest;
import com.kauniv.lightrip.domain.team.dto.request.TeamCreateRequest;
import com.kauniv.lightrip.domain.team.dto.request.TeamJoinRequest;
import com.kauniv.lightrip.domain.team.dto.response.LiveLocationResponse;
import com.kauniv.lightrip.domain.team.dto.response.MyTeamResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamMemberResponse;
import com.kauniv.lightrip.domain.team.dto.response.TeamResponse;
import com.kauniv.lightrip.domain.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "팀 API", description = "팀 생성, 가입, 조회, 탈퇴 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다. 생성자는 자동으로 LEADER가 됩니다.")
    @PostMapping
    public ResponseEntity<TeamResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody TeamCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.create(userId, req));
    }

    @Operation(summary = "팀 코드로 검색", description = "팀 코드로 팀을 검색합니다. 가입 전 팀 정보를 미리 확인할 때 사용합니다.")
    @GetMapping("/search")
    public ResponseEntity<TeamResponse> searchByCode(
            @Parameter(description = "검색할 팀 코드 (8자리 대문자)") @RequestParam String code) {
        return ResponseEntity.ok(teamService.searchByCode(code));
    }

    @Operation(summary = "팀 가입", description = "팀 코드를 입력해 팀에 가입합니다. 코드가 일치하면 즉시 MEMBER로 추가됩니다.")
    @PostMapping("/join")
    public ResponseEntity<TeamResponse> join(
            @AuthenticationPrincipal Long userId,
            @RequestBody TeamJoinRequest req) {
        return ResponseEntity.ok(teamService.join(userId, req));
    }

    @Operation(summary = "내 팀 조회",
            description = "로그인한 사용자가 소속된 팀 정보를 조회합니다. 팀 ID, 팀장, 전체 팀원 목록을 반환합니다. 소속된 팀이 없으면 404를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<MyTeamResponse> getMyTeam(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(teamService.getMyTeam(userId));
    }

    @Operation(summary = "팀 정보 조회", description = "팀 ID로 팀 기본 정보를 조회합니다.")
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(
            @Parameter(description = "조회할 팀의 ID") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @Operation(summary = "팀 멤버 목록 조회", description = "해당 팀의 전체 멤버 목록과 역할(LEADER/MEMBER)을 조회합니다.")
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(
            @Parameter(description = "조회할 팀의 ID") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getMembers(teamId));
    }

    @Operation(summary = "팀 탈퇴", description = "팀에서 탈퇴합니다. LEADER는 탈퇴할 수 없습니다.")
    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "탈퇴할 팀의 ID") @PathVariable Long teamId) {
        teamService.leave(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팀 실시간 위치 조회", description = "현재 위치를 공유 중인 팀원 목록을 반환합니다. 5분 이상 업데이트 없으면 목록에서 제외됩니다.")
    @GetMapping("/{teamId}/live-locations")
    public ResponseEntity<List<LiveLocationResponse>> getLiveLocations(
            @Parameter(description = "조회할 팀의 ID") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getLiveLocations(teamId));
    }

    @Operation(summary = "위치 공유 온오프", description = "위치 공유를 켜거나 끕니다. false 시 즉시 팀원 지도에서 제외됩니다.")
    @PatchMapping("/{teamId}/live-locations/me")
    public ResponseEntity<Void> updateLocationSharing(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @RequestBody LocationSharingRequest req) {
        teamService.updateLocationSharing(userId, teamId, req.sharing());
        return ResponseEntity.noContent().build();
    }
}