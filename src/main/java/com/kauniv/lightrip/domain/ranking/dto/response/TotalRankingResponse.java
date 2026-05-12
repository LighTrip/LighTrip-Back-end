package com.kauniv.lightrip.domain.ranking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "전체 주간 랭킹 응답")
public record TotalRankingResponse(

        @Schema(description = "TOP 10 랭킹 목록")
        List<RankingResponse> topRankings,

        @Schema(description = "내 순위 (TOP 10 안이면 null, 점수 0점이면 null)")
        RankingResponse myRank
) {
}