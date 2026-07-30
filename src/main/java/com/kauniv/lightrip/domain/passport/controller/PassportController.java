package com.kauniv.lightrip.domain.passport.controller;

import com.kauniv.lightrip.domain.passport.dto.request.PassportCreateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportUpdateRequest;
import com.kauniv.lightrip.domain.passport.dto.request.PassportVisibilityRequest;
import com.kauniv.lightrip.domain.passport.dto.response.PassportResponse;
import com.kauniv.lightrip.domain.passport.dto.response.PassportStatsResponse;
import com.kauniv.lightrip.domain.passport.service.PassportService;
import com.kauniv.lightrip.global.ai.dto.AiDraftRequest;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.service.AiService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import com.kauniv.lightrip.domain.passport.dto.response.FeedPassportResponse;
import com.kauniv.lightrip.global.common.response.FeedCursorResponse;
import java.math.BigDecimal;

@Tag(name = "Passport", description = "여권 API")
@RestController
@RequestMapping("/api/v1/passports")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;
    private final AiService aiService;
    // > AI 초안 생성 담당. 초안 경로가 /api/v1/passports 하위라 이 컨트롤러에서 함께 노출한다.

    @Operation(summary = "AI 여권 초안 생성",
            description = """
                    방문 정보로 여권 본문 초안을 생성합니다. 한국어 2~4문장, 담백한 여행 기록 톤.

                    ### ✅ 필수 항목
                    | 필드 | 설명 |
                    |---|---|
                    | `spaceName` | 장소명 (최대 50자) |
                    | `category` | 카테고리 enum (CAFE/RESTAURANT/BAR/CULTURE/ACTIVITY/SHOPPING/NATURE/ETC) |
                    | `districtCategory` | 권역 enum (예: MAPO, GANGNAM, SEONGNAM_BUNDANG) |
                    | `visitedAt` | 방문 날짜 (`YYYY-MM-DD`, 오늘 또는 과거만 허용) |

                    ### ⚙️ 선택 항목
                    | 필드 | 동작 |
                    |---|---|
                    | `keywords` | 초안에 반영할 키워드. 최대 5개, 각 20자 이내. 생략 가능 |

                    ### 🚨 자주 걸리는 에러
                    | 코드 | 상황 |
                    |---|---|
                    | 400 `C001` | enum 오타 / `visitedAt`이 미래 / 장소명 누락 |
                    | 401 `A001` | Authorization 헤더 누락 |
                    | 429 `AI003` | AI 요청량 초과 — 잠시 후 재시도 |
                    | 502 `AI001` | AI 호출 실패 (재시도 1회 후에도 실패) |
                    | 502 `AI002` | AI 응답 해석 실패 |

                    ### 🔁 동작 흐름
                    1. 본인의 과거 기록 중 유사한 글을 찾아 어투 참고자료로 사용 (없으면 생략)
                    2. 초안 생성 후 `draft` 반환. `category`는 요청값을 그대로 되돌려줌
                    3. 받은 값을 여권 등록 API의 `draft` / `aiCategory`에 담아 보내면 원본이 보존됨

                    > ⚠️ 카테고리 자동 분류는 지원하지 않습니다. `category`는 요청값 그대로입니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초안 생성 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "SUCCESS",
                              "message": "초안이 생성되었습니다.",
                              "data": {
                                "draft": "마포구 안녕커피에서 오후를 보냈다. 창가 자리에 앉아 드립커피를 천천히 마셨다. 오랜만에 여유로운 시간이었다.",
                                "category": "CAFE"
                              }
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429", description = "AI 요청량 초과",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "code": "AI003",
                              "message": "AI 요청이 많아 잠시 후 다시 시도해주세요."
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "AI 호출 또는 응답 해석 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "code": "AI001",
                              "message": "AI 초안 생성에 실패했습니다."
                            }
                            """)))
    })
    @PostMapping("/draft")
    public ApiResponse<AiDraftResponse> generateDraft(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiDraftRequest request
    ) {
        return ApiResponse.success("초안이 생성되었습니다.", aiService.generateDraft(userId, request));
    }

    @Operation(summary = "여권 등록",
            description = """
                    방문 기록을 여권으로 등록합니다.

                    ### ✅ 필수 항목
                    | 필드 | 설명 |
                    |---|---|
                    | `imageUrls` | 이미지 URL 배열 (1~5장). S3 presigned URL로 업로드 후 받은 CloudFront URL 사용 |
                    | `content` | 사용자가 최종 작성한 기록 내용 |
                    | `latitude`, `longitude` | 위·경도 좌표 (소수점 7자리) |
                    | `address` | 전체 주소 (최대 50자) |
                    | `visitedAt` | 방문 날짜 (`YYYY-MM-DD`, 오늘 또는 과거만 허용) |
                    | `category` | 카테고리 enum (CAFE/RESTAURANT/BAR/CULTURE/ACTIVITY/SHOPPING/NATURE/ETC) |
                    | `districtCategory` | 권역 enum. 서울 25구 + 경기 시·군(구가 있는 시는 구 단위) (예: MAPO, GANGNAM, SEONGNAM_BUNDANG) |

                    ### ⚙️ 선택 항목 (생략 가능 / null 허용)
                    | 필드 | 동작 |
                    |---|---|
                    | `visibility` | 미입력 시 **PUBLIC**. (PUBLIC / FRIENDS_ONLY / PRIVATE) |
                    | `draft` | AI 초안 원본. AI 미사용이면 생략 — 학습 데이터 보존용 |
                    | `aiCategory` | AI가 분류한 카테고리 초기값 (사용자가 `category`를 바꿔도 보존) |
                    | `district` | 행정구역 표시명 (예: "마포구"). UI 표시용, 생략 가능 |
                    | `spaceName` | 위치명 (예: "안녕커피") — 최대 50자 |
                    | `musicTitle`, `musicArtist` | 함께 듣던 음악 메타. 최대 100자 |
                    | `teamId` | 팀 여권으로 등록하려면 팀 ID 지정. **개인 여권이면 생략 또는 `null`** (`0` 금지 — TEAM_NOT_FOUND) |

                    ### 🚨 자주 걸리는 에러
                    | 코드 | 상황 |
                    |---|---|
                    | 400 `C001` | enum 오타 (소문자 등) / `visitedAt`이 미래 / 이미지 0장 또는 6장 이상 / 주소 50자 초과 |
                    | 401 `A001` | Authorization 헤더 누락 |
                    | 403 `P002` | `teamId` 지정했는데 본인이 그 팀 멤버 아님 |
                    | 409 `P003` | 같은 `(userId, latitude, longitude, visitedAt)` 조합 중복 |

                    ### 🔁 동작 흐름
                    1. (선택) AI 초안: `POST /api/v1/passports/draft` → `draft`, `aiCategory` 받기
                    2. 이미지 업로드: `POST /api/v1/images/presigned-url` → presigned URL로 S3 PUT → 받은 CloudFront URL을 `imageUrls`에 담음
                    3. 본 API 호출: 위 정보를 모아서 등록
                    4. 등록된 여권의 지역이 **첫 등록**이면 `DistrictCover`도 자동 생성 (첫 이미지가 커버로)
                    """)
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

    @Operation(summary = "여권 공개 범위 변경",
            description = "여권의 공개 범위만 단독으로 변경합니다. 작성자 본인만 변경 가능합니다. (PUBLIC / FRIENDS_ONLY / PRIVATE)")
    @PatchMapping("/{passportId}/visibility")
    public ApiResponse<PassportResponse> updateVisibility(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "여권 ID") @PathVariable Long passportId,
            @Valid @RequestBody PassportVisibilityRequest request
    ) {
        return ApiResponse.success("공개 범위가 변경되었습니다.", passportService.updateVisibility(userId, passportId, request));
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
            description = "내가 여권을 등록한 지역 목록을 조회합니다. 지역별 여권 수와 대표 이미지를 포함합니다. "
                    + "teamId 입력 시 해당 팀의 여권 기준으로 조회합니다(팀 모드, 커버 정보는 없음).")
    @GetMapping("/districts/me")
    public ApiResponse<List<DistrictResponse>> getMyDistricts(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "팀 ID (팀 모드, 미입력 시 개인)") @RequestParam(required = false) Long teamId
    ) {
        return ApiResponse.success(passportService.getMyDistricts(userId, teamId));
    }

    @Operation(
            summary = "내 여권 목록 조회 (장소별)",
            description = """
                내가 등록한 여권을 최신순으로 조회합니다.
                
                **필터링**
                - `category`: 카테고리별 필터 (CAFE, RESTAURANT, BAR, TOURIST, NATURE, CULTURE, ACTIVITY, ACCOMMODATION, SHOPPING, ETC)
                - `districtCategory`: 지역별 필터 (MAPO, GANGNAM, SEONGNAM_BUNDANG 등 — 경기 구가 있는 시는 구 단위)
                
                **페이징 (커서 기반 무한스크롤)**
                - 첫 요청: `cursor` 파라미터 없이 호출
                - 다음 페이지: 응답의 `nextCursor` 값을 `cursor`에 넣어서 호출
                - `hasNext`가 `false`이면 마지막 페이지입니다.
                """)
    @GetMapping("/me")
    public ApiResponse<CursorResponse<PassportListResponse>> getMyPassports(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "팀 ID (팀 모드, 미입력 시 개인)") @RequestParam(required = false) Long teamId,
            @Parameter(description = "카테고리 필터 (선택)") @RequestParam(required = false) Category category,
            @Parameter(description = "지역 필터 (선택)") @RequestParam(required = false) District districtCategory,
            @Parameter(description = "커서 (이전 응답의 nextCursor 값). 첫 요청 시 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 페이지에 가져올 여권 수 (기본값: 10, 최대: 50)") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(passportService.getMyPassports(userId, teamId, category, districtCategory, cursor, size));
    }

    @Operation(summary = "내 여권 통계 조회",
            description = "내가 작성한 여권 수, 좋아요 누른 수, 스크랩한 수를 조회합니다. "
                    + "teamId 입력 시 팀 모드: 팀 여권 수 + 팀 여권이 받은 좋아요/스크랩 합계를 반환합니다.")
    @GetMapping("/stats/me")
    public ApiResponse<PassportStatsResponse> getMyStats(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "팀 ID (팀 모드, 미입력 시 개인)") @RequestParam(required = false) Long teamId
    ) {
        return ApiResponse.success(passportService.getMyStats(userId, teamId));
    }

    @Operation(summary = "카테고리별 내 여권 조회",
            description = "특정 카테고리의 내 여권 목록을 조회합니다. 잘못된 카테고리 값 입력 시 400을 반환합니다.")
    @GetMapping("/categories/{category}")
    public ApiResponse<CursorResponse<PassportListResponse>> getMyPassportsByCategory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "카테고리 (CAFE, RESTAURANT, BAR, CULTURE, ACTIVITY, SHOPPING, NATURE, ETC)")
            @PathVariable Category category,
            @Parameter(description = "팀 ID (팀 모드, 미입력 시 개인)") @RequestParam(required = false) Long teamId,
            @Parameter(description = "커서 (이전 응답의 nextCursor 값). 첫 요청 시 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 페이지에 가져올 여권 수 (기본값: 10)") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(passportService.getMyPassports(userId, teamId, category, null, cursor, size));
    }

    @Operation(summary = "지역별 내 여권 상세 조회",
            description = """
                    특정 지역(districtCategory)의 내 여권 상세 정보를 커서 기반으로 페이징 조회합니다.

                    - 정렬: 방문일(visitedAt) 내림차순, 동일 시 ID 내림차순
                    - 페이지 크기: 기본 10
                    - 응답: 여권 상세 전체 필드 (이미지/본문/좌표/음악/도장 등 `PassportResponse`)
                    - 첫 요청: `cursor` 생략, 다음 페이지: 이전 응답의 `nextCursor` 사용
                    - 잘못된 districtCategory 값 입력 시 400을 반환합니다.
                    """)
    @GetMapping("/districts/{districtCategory}")
    public ApiResponse<CursorResponse<PassportResponse>> getMyPassportDetailsByDistrict(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "지역 카테고리 (MAPO, GANGNAM, SEONGNAM_BUNDANG 등)")
            @PathVariable District districtCategory,
            @Parameter(description = "팀 ID (팀 모드, 미입력 시 개인)") @RequestParam(required = false) Long teamId,
            @Parameter(description = "커서 (이전 응답의 nextCursor 값). 첫 요청 시 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 페이지에 가져올 여권 수 (기본값: 10)") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                passportService.getMyPassportDetailsByDistrict(userId, teamId, districtCategory, cursor, size)
        );
    }

    @Operation(summary = "릴스형 여권 피드 조회",
            description = "다른 사용자의 PUBLIC 여권을 인기순(+약간의 랜덤)으로 조회합니다. 카테고리/지역/위치 필터 지원, 커서 기반 무한스크롤. "
                    + "seed를 보내면 인기순을 유지하면서 비슷한 점수끼리 섞여 매번 다른 순서가 됩니다. "
                    + "무한스크롤 동안에는 같은 seed를 유지하고, 새로고침 시 새 seed를 생성하세요. 미입력 시 순수 인기순입니다.")
    @GetMapping("/feed")
    public ApiResponse<FeedCursorResponse<FeedPassportResponse>> getFeed(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "카테고리 필터") @RequestParam(required = false) Category category,
            @Parameter(description = "지역 필터") @RequestParam(required = false) District district,
            @Parameter(description = "사용자 위도") @RequestParam(required = false) BigDecimal latitude,
            @Parameter(description = "사용자 경도") @RequestParam(required = false) BigDecimal longitude,
            @Parameter(description = "반경(km), 기본 5") @RequestParam(defaultValue = "5") int radius,
            @Parameter(description = "셔플 시드 (피드 세션마다 동일 값 유지, 미입력 시 인기순)") @RequestParam(required = false) String seed,
            @Parameter(description = "커서 (마지막 여권 ID)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "커서 점수 (마지막 응답의 nextCursorScore)") @RequestParam(required = false) Long cursorScore,
            @Parameter(description = "조회 개수, 기본 10") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                passportService.getFeed(userId, category, district, latitude, longitude,
                        radius, seed, cursor, cursorScore, size)
        );
    }

}