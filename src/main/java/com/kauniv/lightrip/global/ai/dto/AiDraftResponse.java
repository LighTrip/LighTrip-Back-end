package com.kauniv.lightrip.global.ai.dto;

import com.kauniv.lightrip.global.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 여권 초안 생성 응답")
public record AiDraftResponse(

        @Schema(
                description = "AI가 생성한 기록 초안. 프론트에서 content 입력창에 미리 채워줄 값.",
                example = "안녕커피에서 오후를 보냈다. 창가 자리에 앉아 드립커피를 천천히 마셨다."
        )
        String draft,

        @Schema(
                description = "AI가 분류한 카테고리. 프론트에서 카테고리 선택창에 미리 선택해줄 값. "
                        + "판단이 애매하면 `ETC`로 온다. "
                        + "여권 등록 시 `aiCategory`로 함께 보내면 초안 원본과 짝지어 보존된다.",
                example = "CAFE"
        )
        Category category
) {}
