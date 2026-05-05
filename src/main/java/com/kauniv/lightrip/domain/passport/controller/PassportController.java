package com.kauniv.lightrip.domain.passport.controller;

import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
import com.kauniv.lightrip.domain.passport.dto.response.PassportResponse;
import com.kauniv.lightrip.domain.passport.dto.response.PassportStatsResponse;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.kauniv.lightrip.domain.passport.dto.response.DistrictResponse;
import java.util.List;
import com.kauniv.lightrip.domain.passport.dto.response.PassportListResponse;
import com.kauniv.lightrip.global.common.response.CursorResponse;
import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;


@Tag(name = "Passport", description = "여권 API")
@RestController
@RequestMapping("/api/v1/passports")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;

    @Operation(summary = "여권 등록",
            description = "방문 기록을 여권으로 등록합니다. 이미지 1~5장 필수.")
    @PostMapping
    public ApiResponse<PassportResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PassportCreateRequest request
    ) {
        return ApiResponse.success("여권이 등록되었습니다.", passportService.create(userId, request));
    }

    @Operation(summary = "여권 수정",
            description = "여권 정보를 수정합니다. 위치/날짜 수정 불가. 개인=본인만, 팀=팀원 누구나.")
    @PatchMapping("/{passportId}")
    public ApiResponse<PassportResponse> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId,
            @Valid @RequestBody PassportUpdateRequest request
    ) {
        return ApiResponse.success("여권이 수정되었습니다.", passportService.update(userId, passportId, request));
    }

    @Operation(summary = "여권 삭제",
            description = "여권을 삭제합니다. 작성자만 삭제 가능하며, 연관 스크랩도 함께 삭제됩니다.")
    @DeleteMapping("/{passportId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        passportService.delete(userId, passportId);
        return ApiResponse.success("여권이 삭제되었습니다.", null);
    }

    @Operation(summary = "여권 상세 조회",
            description = "여권 상세 정보를 조회합니다. 공개 범위(visibility)에 따라 접근이 제한됩니다.")
    @GetMapping("/{passportId}")
    public ApiResponse<PassportResponse> getPassport(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId
    ) {
        return ApiResponse.success(passportService.getPassport(userId, passportId));
    }

    @Operation(summary = "내 기록 지역 조회",
            description = "내가 여권을 등록한 지역 목록을 조회합니다. 지역별 여권 수와 대표 이미지를 포함합니다.")
    @GetMapping("/districts/me")
    public ApiResponse<List<DistrictResponse>> getMyDistricts(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(passportService.getMyDistricts(userId));
    }

    @Operation(
            summary = "내 여권 목록 조회 (장소별)",
            description = """
                내가 등록한 여권을 최신순으로 조회합니다.
                
                **필터링**
                - `category`: 카테고리별 필터 (CAFE, RESTAURANT, BAR, TOURIST, NATURE, CULTURE, ACTIVITY, ACCOMMODATION, SHOPPING, ETC)
                - `districtCategory`: 지역별 필터 (MAPO, GANGNAM, YONGSAN 등)
                - 필터는 복수 적용 가능합니다. (예: category=CAFE & districtCategory=MAPO → 마포구 카페만)
                - 필터를 넣지 않으면 전체 여권이 조회됩니다.
                
                **페이징 (커서 기반 무한스크롤)**
                - 첫 요청: `cursor` 파라미터 없이 호출
                - 다음 페이지: 응답의 `nextCursor` 값을 `cursor`에 넣어서 호출
                - `hasNext`가 `false`이면 마지막 페이지입니다.
                """)
    @GetMapping("/me")
    public ApiResponse<CursorResponse<PassportListResponse>> getMyPassports(
            @AuthenticationPrincipal Long userId,

            @Parameter(description = "카테고리 필터 (선택). 예: CAFE, RESTAURANT, BAR, TOURIST, NATURE, CULTURE, ACTIVITY, ACCOMMODATION, SHOPPING, ETC")
            @RequestParam(required = false) Category category,

            @Parameter(description = "지역 필터 (선택). 예: MAPO, GANGNAM, YONGSAN, JONGNO 등")
            @RequestParam(required = false) District districtCategory,

            @Parameter(description = "커서 (이전 응답의 nextCursor 값). 첫 요청 시 생략")
            @RequestParam(required = false) Long cursor,

            @Parameter(description = "한 페이지에 가져올 여권 수 (기본값: 10, 최대: 50)")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(passportService.getMyPassports(userId, category, districtCategory, cursor, size));
    }


    @Operation(summary = "내 여권 통계 조회",
            description = "내가 작성한 여권 수, 좋아요 누른 수, 스크랩한 수를 조회합니다.")
    @GetMapping("/stats/me")
    public ApiResponse<PassportStatsResponse> getMyStats(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(passportService.getMyStats(userId));
    }

}