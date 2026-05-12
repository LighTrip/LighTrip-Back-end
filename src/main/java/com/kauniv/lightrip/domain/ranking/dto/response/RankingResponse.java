package com.kauniv.lightrip.domain.ranking.dto.response;

import com.kauniv.lightrip.domain.ranking.entity.Ranking;
import com.kauniv.lightrip.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랭킹 항목")
public record RankingResponse(

        @Schema(description = "순위")
        Integer rank,

        @Schema(description = "사용자 ID")
        Long userId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "주간 점수 (좋아요×2 + 스크랩×3)")
        Integer score
) {
    public static RankingResponse from(Ranking ranking) {
        User user = ranking.getUser();
        return new RankingResponse(
                ranking.getRankingPosition(),
                user.getId(),
                user.getNickname(),
                user.getProfileImg(),
                ranking.getScore()
        );
    }
}