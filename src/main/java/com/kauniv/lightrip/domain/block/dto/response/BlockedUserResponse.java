package com.kauniv.lightrip.domain.block.dto.response;

import com.kauniv.lightrip.domain.block.entity.UserBlock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "차단한 사용자 항목")
public record BlockedUserResponse(

        @Schema(description = "차단 기록 ID (커서로 사용)")
        Long blockId,

        @Schema(description = "차단한 상대 사용자 ID")
        Long userId,

        @Schema(description = "상대 닉네임")
        String nickname,

        @Schema(description = "상대 프로필 이미지 URL")
        String profileImg,

        @Schema(description = "차단 시각")
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock block) {
        return new BlockedUserResponse(
                block.getId(),
                block.getBlocked().getId(),
                block.getBlocked().getNickname(),
                block.getBlocked().getProfileImg(),
                block.getCreatedAt()
        );
    }
}
