package com.kauniv.lightrip.domain.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FriendStatusUpdate(
        @Schema(description = "친구 요청 처리 액션", allowableValues = {"ACCEPT", "REJECT"}, example = "ACCEPT")
        @NotNull(message = "액션은 필수입니다.")
        FriendAction action
) {}