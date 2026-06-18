package com.kauniv.lightrip.domain.ranking.controller;

import com.kauniv.lightrip.domain.ranking.dto.response.RankingResponse;
import com.kauniv.lightrip.domain.ranking.dto.response.TotalRankingResponse;
import com.kauniv.lightrip.domain.ranking.service.RankingService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import com.kauniv.lightrip.global.enums.District;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Ranking", description = "랭킹 API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @Operation(
            summary = "전체 주간 랭킹 조회",
            description = """
                    지난 1주일간 좋아요(×2)와 스크랩(×3)을 합산한 주간 랭킹을 조회합니다.
                    
                    - 상위 10명의 랭킹 목록을 반환합니다.
                    - 본인이 11위 이하인 경우 myRank에 별도 포함됩니다.
                    - 본인이 TOP 10 안이거나 점수 0점이면 myRank는 null입니다.
                    - 매주 일요일 21:00에 자동 갱신됩니다.
                    """
    )
    @GetMapping("/total")
    public ApiResponse<TotalRankingResponse> getTotalRanking(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(rankingService.getTotalRanking(userId));
    }

    @Operation(
            summary = "구별 주간 랭킹 조회",
            description = """
                    지난 1주일간 해당 구의 여권으로 받은 좋아요(×2)와 스크랩(×3)을 합산한 구별 주간 랭킹을 조회합니다.

                    - 상위 10명의 랭킹 목록을 반환합니다.
                    - 매주 일요일 21:00에 자동 갱신됩니다.
                    - 잘못된 district 값 입력 시 400을 반환합니다.
                    """
    )
    @GetMapping("/districts/{district}")
    public ApiResponse<List<RankingResponse>> getDistrictRanking(
            @Parameter(description = "지역 카테고리 (MAPO, GANGNAM, SEONGNAM_BUNDANG 등)")
            @PathVariable District district
    ) {
        return ApiResponse.success(rankingService.getDistrictRanking(district));
    }
}