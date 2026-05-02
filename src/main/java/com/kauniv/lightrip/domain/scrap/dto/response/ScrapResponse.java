package com.kauniv.lightrip.domain.scrap.dto.response;

import com.kauniv.lightrip.domain.scrap.entity.Scrap;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "스크랩 응답")
public record ScrapResponse(
        Long scrapId,
        Long passportId,
        LocalDateTime createdAt
) {
    public static ScrapResponse from(Scrap scrap) {
        return new ScrapResponse(
                scrap.getId(),
                scrap.getPassport().getId(),
                scrap.getCreatedAt()
        );
    }
}