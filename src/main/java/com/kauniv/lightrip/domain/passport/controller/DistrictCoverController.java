package com.kauniv.lightrip.domain.passport.controller;

import com.kauniv.lightrip.domain.passport.dto.request.DistrictCoverTextColorRequest;
import com.kauniv.lightrip.domain.passport.service.DistrictCoverService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "DistrictCover", description = "지역 커버 API")
@RestController
@RequestMapping("/api/v1/district-covers")
@RequiredArgsConstructor
public class DistrictCoverController {

    private final DistrictCoverService districtCoverService;

    @Operation(summary = "지역 커버 텍스트 색상 변경",
            description = "커버 이미지 위에 표시되는 지역명 텍스트 색상을 HEX 코드로 변경합니다. 본인 소유 커버만 변경 가능합니다.")
    @PatchMapping("/{coverId}/text-color")
    public ApiResponse<Void> updateTextColor(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "지역 커버 ID") @PathVariable Long coverId,
            @Valid @RequestBody DistrictCoverTextColorRequest request
    ) {
        districtCoverService.updateTextColor(userId, coverId, request);
        return ApiResponse.success("텍스트 색상이 변경되었습니다.", null);
    }
}
