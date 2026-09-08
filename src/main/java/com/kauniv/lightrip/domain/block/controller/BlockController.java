package com.kauniv.lightrip.domain.block.controller;

import com.kauniv.lightrip.domain.block.dto.response.BlockedUserResponse;
import com.kauniv.lightrip.domain.block.service.BlockService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "차단 API", description = "사용자 차단 / 차단 해제 / 차단 목록 조회. UGC 콘텐츠 정책 대응.")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @Operation(summary = "사용자 차단",
            description = """
                    해당 사용자를 차단합니다.

                    - 자기 자신 차단 불가 (400 `BL001`)
                    - 이미 차단한 상대면 멱등 처리 (200)
                    - 차단 시 두 사용자의 친구 관계 및 대기 중인 친구 요청이 해제됩니다.
                    - 차단 후 서로의 게시물·프로필·추천·검색 노출이 차단됩니다. (팀 내부는 예외)
                    """)
    @PostMapping("/{userId}/block")
    public ApiResponse<Void> block(
            @AuthenticationPrincipal Long me,
            @Parameter(description = "차단할 사용자 ID") @PathVariable Long userId
    ) {
        blockService.block(me, userId);
        return ApiResponse.success("차단했습니다.", null);
    }

    @Operation(summary = "차단 해제",
            description = "차단을 해제합니다. 이전 친구 관계는 복원되지 않습니다. 차단 기록이 없으면 404 `BL002`.")
    @DeleteMapping("/{userId}/block")
    public ApiResponse<Void> unblock(
            @AuthenticationPrincipal Long me,
            @Parameter(description = "차단 해제할 사용자 ID") @PathVariable Long userId
    ) {
        blockService.unblock(me, userId);
        return ApiResponse.success("차단을 해제했습니다.", null);
    }

    @Operation(summary = "내 차단 목록 조회",
            description = "내가 차단한 사용자 목록을 최신순 커서 페이징으로 조회합니다.")
    @GetMapping("/me/blocks")
    public ApiResponse<CursorResponse<BlockedUserResponse>> getMyBlocks(
            @AuthenticationPrincipal Long me,
            @Parameter(description = "커서 (이전 응답의 nextCursor). 첫 요청 시 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 20)") @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(blockService.getMyBlocks(me, cursor, size));
    }
}
