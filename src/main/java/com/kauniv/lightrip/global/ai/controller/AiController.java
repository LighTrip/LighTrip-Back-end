package com.kauniv.lightrip.global.ai.controller;

import com.kauniv.lightrip.global.ai.dto.AiDraftRequest;
import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.service.AiService;
import com.kauniv.lightrip.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 초안 생성 API")
@RestController
@RequestMapping("/api/v1/passports")
// > 경로는 여권 하위(/api/v1/passports/draft)를 그대로 유지하고 Swagger 태그만 분리한다.
// > PassportController에 두면 여권 CRUD 문서에 섞여서 찾기 어려움.
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

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
                    | `imageUrl` | 방문 사진 URL. 넣으면 사진 내용까지 반영. **공개 접근 가능한 https URL만** |

                    ### 🚨 자주 걸리는 에러
                    | 코드 | 상황 |
                    |---|---|
                    | 400 `C001` | enum 오타 / `visitedAt`이 미래 / 장소명 누락 / `imageUrl`이 https 아님 |
                    | 400 `AI004` | 사진을 못 불러옴 — 죽은 링크이거나 비공개 URL |
                    | 401 `A001` | Authorization 헤더 누락 |
                    | 429 `AI003` | AI 요청량 초과 — 잠시 후 재시도 |
                    | 502 `AI001` | AI 호출 실패 (재시도 1회 후에도 실패) |
                    | 502 `AI002` | AI 응답 해석 실패 |

                    ### 🔁 동작 흐름
                    1. 본인의 과거 기록 중 유사한 글을 찾아 어투 참고자료로 사용 (없으면 생략)
                    2. `imageUrl`이 있으면 사진을 함께 넘겨 초안 생성 (없으면 텍스트만)
                    3. 초안 생성 후 `draft` 반환. `category`는 요청값을 그대로 되돌려줌
                    4. 받은 값을 여권 등록 API의 `draft` / `aiCategory`에 담아 보내면 원본이 보존됨

                    > ⚠️ 사진은 OpenAI 서버가 직접 받아갑니다. S3 presigned URL(만료됨)이 아니라
                    > 업로드 완료 후의 CloudFront URL을 넣어야 합니다.

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
                    responseCode = "400", description = "사진을 불러오지 못함",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "code": "AI004",
                              "message": "사진을 불러올 수 없습니다. 공개된 이미지 URL인지 확인해주세요."
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
}
