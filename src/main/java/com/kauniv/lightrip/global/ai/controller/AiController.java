package com.kauniv.lightrip.global.ai.controller;

import com.kauniv.lightrip.global.ai.dto.AiDraftResponse;
import com.kauniv.lightrip.global.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 초안 생성 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 초안 생성",
            description = """
                    사진(+메모)으로 여권 본문 초안과 카테고리 초기값을 생성합니다.
                    한국어 2~4문장, 일기 쓰듯 편한 반말 톤.

                    ### ✅ 파라미터
                    | 이름 | 필수 | 설명 |
                    |---|---|---|
                    | `imageUrl` | O | 방문 사진 URL. **공개 접근 가능한 https URL만** |
                    | `text` | X | 사용자가 입력한 메모. 넣으면 과거 기록 기반 RAG 경로로 개인화됨 |

                    ### 🚨 자주 걸리는 에러
                    | 코드 | 상황 |
                    |---|---|
                    | 400 `C001` | `imageUrl` 누락 |
                    | 400 `AI004` | 사진을 못 불러옴 — 죽은 링크이거나 비공개 URL |
                    | 401 `A001` | Authorization 헤더 누락 |
                    | 429 `AI003` | AI 요청량 초과 — 잠시 후 재시도 |
                    | 502 `AI001` | AI 호출 실패 (재시도 1회 후에도 실패) |
                    | 502 `AI002` | AI 응답 해석 실패 |

                    ### 🔁 동작 흐름
                    1. `text`가 있으면 본인의 유사한 과거 기록을 찾아 어투 참고자료로 사용 (없으면 생략)
                    2. 사진과 메모로 초안 생성 + 카테고리 분류
                    3. 받은 값을 여권 등록 API의 `draft` / `aiCategory`에 담아 보내면 원본이 보존됨

                    > ⚠️ 사진은 OpenAI 서버가 직접 받아갑니다. S3 presigned URL(만료됨)이 아니라
                    > 업로드 완료 후의 CloudFront URL을 넣어야 합니다.

                    > ⚠️ `category`는 AI가 분류한 값입니다. 애매하면 `ETC`로 옵니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초안 생성 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "draft": "오늘은 안녕커피에 갔다. 창가 자리에 앉아서 드립커피를 천천히 마셨는데 오랜만에 여유로웠다.",
                              "category": "CAFE"
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
    public ResponseEntity<AiDraftResponse> generateDraft(
            @RequestParam String imageUrl,
            @RequestParam(required = false) String text,
            // > 사용자가 입력한 메모 텍스트. RAG 경로 활성화에 사용. optional.
            @AuthenticationPrincipal Long userId
            // > JWT에서 추출한 userId. 본인 과거 기록만 검색하기 위해 사용.
    ) {
        return ResponseEntity.ok(aiService.generateDraft(imageUrl, text, userId));
    }
}
