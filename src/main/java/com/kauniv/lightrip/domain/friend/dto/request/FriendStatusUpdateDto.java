package com.kauniv.lightrip.domain.friend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FriendStatusUpdateDto(
        @NotBlank(message = "액션은 필수입니다.")
        String action
) {}