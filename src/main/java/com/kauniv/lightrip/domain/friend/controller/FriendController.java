package com.kauniv.lightrip.domain.friend.controller;

import com.kauniv.lightrip.domain.friend.dto.request.FriendRequestDto;
import com.kauniv.lightrip.domain.friend.dto.request.FriendStatusUpdateDto;
import com.kauniv.lightrip.domain.friend.dto.response.FriendResponseDto;
import com.kauniv.lightrip.domain.friend.service.FriendService;
import com.kauniv.lightrip.global.oauth.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request")
    public ResponseEntity<FriendResponseDto> sendRequest(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody FriendRequestDto dto) {

        FriendResponseDto response = friendService.sendRequest(user.getUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{friendId}")
    public ResponseEntity<?> handleRequest(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long friendId,
            @Valid @RequestBody FriendStatusUpdateDto dto) {

        FriendResponseDto response = friendService.handleRequest(user.getUserId(), friendId, dto);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long friendId) {

        friendService.deleteFriend(user.getUserId(), friendId);
        return ResponseEntity.noContent().build();
    }
}