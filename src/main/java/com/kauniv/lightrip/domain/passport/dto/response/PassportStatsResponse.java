package com.kauniv.lightrip.domain.passport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 여권 통계 응답")
public record PassportStatsResponse(

        @Schema(description = "작성한 여권 수", example = "26")
        Long passportCount,

        @Schema(description = "좋아요 누른 수", example = "18")
        Long likeCount,

        @Schema(description = "스크랩한 수", example = "7")
        Long scrapCount
) {}