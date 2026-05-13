package com.kauniv.lightrip.domain.team.dto.response;

import java.math.BigDecimal;

public record TeamMapResponse(
        Long userId,
        String nickname,
        BigDecimal latitude,
        BigDecimal longitude
) {}