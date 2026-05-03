package com.kauniv.lightrip.domain.friend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestDto(
        @NotBlank(message = "친구 코드는 필수입니다.")
        String friendCode
) {}