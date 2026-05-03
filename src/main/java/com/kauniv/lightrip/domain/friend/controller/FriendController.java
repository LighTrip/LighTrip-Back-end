package com.kauniv.lightrip.domain.friend.controller;

import com.kauniv.lightrip.domain.friend.dto.request.FriendRequestDto;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdateDto;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponseDto;
import com.kauniv.lightrip.domain.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "친구 관리 API", description = "친구 요청, 수락/거절, 삭제 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 요청 보내기", description = "친구 코드로 상대방에게 친구 요청을 보냅니다.")
    @PostMapping("/request")
    public ResponseEntity<FriendResponseDto> sendRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FriendRequestDto dto) {

        FriendResponseDto response = friendService.sendRequest(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "친구 요청 수락/거절", description = "받은 친구 요청을 수락(ACCEPT) 또는 거절(REJECT)합니다.")
    @PatchMapping("/{friendId}")
    public ResponseEntity<?> handleRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId,
            @Valid @RequestBody FriendStatusUpdateDto dto) {

        FriendResponseDto response = friendService.handleRequest(userId, friendId, dto);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다. 양쪽 당사자 모두 삭제 가능합니다.")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId) {

        friendService.deleteFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }
}