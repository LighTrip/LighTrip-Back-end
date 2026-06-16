package com.kauniv.lightrip.domain.friend.controller;

import com.kauniv.lightrip.domain.friend.dto.request.FriendRequest;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdate;
import com.kauniv.lightrip.domain.friend.dto.response.FriendPassportResponse;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponse;
import com.kauniv.lightrip.domain.friend.service.FriendService;
import com.kauniv.lightrip.domain.passport.dto.response.DistrictResponse;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "친구 관리 API", description = "친구 요청, 수락/거절, 삭제, 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 요청 보내기", description = "친구 코드로 상대방에게 친구 요청을 보냅니다.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendResponse>> sendRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FriendRequest dto) {

        FriendResponse response = friendService.sendRequest(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "친구 요청 수락/거절", description = "받은 친구 요청을 수락(ACCEPT) 또는 거절(REJECT)합니다.")
    @PatchMapping("/{friendId}")
    public ResponseEntity<?> handleRequest(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Friend 테이블의 PK (friendshipId)") @PathVariable Long friendId,
            @Valid @RequestBody FriendStatusUpdate dto) {

        FriendResponse response = friendService.handleRequest(userId, friendId, dto);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다. 요청자/수신자 양쪽 모두 삭제 가능합니다.")
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Friend 테이블의 PK (friendshipId)") @PathVariable Long friendId) {

        friendService.deleteFriend(userId, friendId);
        return ApiResponse.success("친구 관계가 삭제되었습니다.", null);
    }

    @Operation(summary = "내 친구 목록 조회", description = "수락된 친구 목록을 전체 조회합니다.")
    @GetMapping
    public ApiResponse<List<FriendResponse>> getFriends(
            @AuthenticationPrincipal Long userId) {

        List<FriendResponse> response = friendService.getFriends(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "받은 친구 요청 조회", description = "아직 수락하지 않은 PENDING 상태의 친구 요청 목록을 조회합니다.")
    @GetMapping("/pending")
    public ApiResponse<List<FriendResponse>> getPendingRequests(
            @AuthenticationPrincipal Long userId) {

        List<FriendResponse> response = friendService.getPendingRequests(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "친구 코드로 유저 검색", description = "친구 코드로 유저를 검색합니다. 친구 요청 전 상대방 확인용입니다.")
    @GetMapping("/search")
    public ApiResponse<FriendResponse> searchByFriendCode(
            @Parameter(description = "검색할 친구 코드 (8자리 대문자)") @RequestParam String code) {

        FriendResponse response = friendService.searchByFriendCode(code);
        return ApiResponse.success(response);
    }

    @Operation(summary = "친구 여권 목록 조회", description = "친구의 공개(PUBLIC) 여권 목록을 조회합니다. ACCEPTED 상태의 친구만 조회 가능하며, PRIVATE 여권은 제외됩니다. district 파라미터로 지역 필터링 가능합니다.")
    @GetMapping("/{friendId}/passports")
    public ApiResponse<List<FriendPassportResponse>> getFriendPassports(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 친구의 userId") @PathVariable Long friendId,
            @Parameter(description = "지역 필터 (ex. MAPO, YONGSAN) — 미입력 시 전체 조회") @RequestParam(required = false) District district,
            @PageableDefault(size = 20, sort = "visitedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<FriendPassportResponse> response =
                friendService.getFriendPassports(userId, friendId, district, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "친구 지도 조회", description = "친구의 공개(PUBLIC) 여권 기준으로 방문한 지역 목록을 조회합니다. ACCEPTED 상태의 친구만 조회 가능합니다.")
    @GetMapping("/{friendId}/map")
    public ApiResponse<List<DistrictResponse>> getFriendDistricts(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 친구의 userId") @PathVariable Long friendId) {

        List<DistrictResponse> response = friendService.getFriendDistricts(userId, friendId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "추천 친구 목록 조회", description = "내가 스크랩한 여권의 작성자 중 아직 친구가 아닌 유저를 최대 4명 랜덤으로 추천합니다.")
    @GetMapping("/recommendations")
    public ApiResponse<List<FriendResponse>> getRecommendedFriends(
            @AuthenticationPrincipal Long userId) {

        List<FriendResponse> response = friendService.getRecommendedFriends(userId);
        return ApiResponse.success(response);
    }
}