package com.kauniv.lightrip.global.ai.prompt;

import com.kauniv.lightrip.global.enums.Category;
import com.kauniv.lightrip.global.enums.District;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 여권 초안 생성 프롬프트. 문구 수정은 이 파일만 고치면 된다.
 */
public final class DraftPrompt {

    private DraftPrompt() {
    }

    public static final String SYSTEM = """
            당신은 여행 기록 앱 '라이트립'의 기록 초안 작성 도우미입니다.
            사용자가 방문한 장소 정보를 받아, 사용자가 직접 쓴 것 같은 짧은 여행 기록 초안을 만듭니다.

            작성 규칙
            - 한국어로 작성합니다.
            - 반드시 2문장 이상 4문장 이하로 씁니다.
            - 담백하고 사실 위주인 여행 기록 톤을 유지합니다. 과장된 감탄사, 광고 문구, 해시태그, 이모지는 쓰지 않습니다.
            - 주어진 정보에 없는 사실(가격, 메뉴, 동행자, 날씨 등)을 지어내지 않습니다.
            - 장소명과 지역은 문장에 자연스럽게 녹입니다. 정보를 나열하듯 반복하지 않습니다.
            - '참고 기록'이 주어지면 어투와 문장 길이만 참고합니다. 내용을 그대로 가져오지 않습니다.
            - 사진이 함께 주어지면 사진에서 분명히 보이는 것만 반영합니다.
              흐릿하거나 확실하지 않은 것은 쓰지 않고, 사진을 설명하는 문장('사진 속에는...')도 쓰지 않습니다.

            출력 형식
            - 아래 형태의 JSON 객체 하나만 출력합니다.
            - 설명, 코드블록, 그 밖의 텍스트를 덧붙이지 않습니다.
            {"draft": "초안 본문"}
            """;

    private static final DateTimeFormatter VISITED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    /**
     * 사용자 메시지 구성. 값이 없는 항목은 줄 자체를 생략해서 모델이 빈칸을 지어내지 않게 한다.
     *
     * @param references 과거 기록 기반 참고 문장. 없으면 null.
     */
    public static String user(String spaceName,
                              Category category,
                              District district,
                              LocalDate visitedAt,
                              List<String> keywords,
                              String references) {
        StringBuilder sb = new StringBuilder("아래 방문 정보로 여행 기록 초안을 작성해주세요.\n\n");

        appendIfPresent(sb, "장소명", spaceName);
        appendIfPresent(sb, "카테고리", category != null ? category.getDisplayName() : null);
        appendIfPresent(sb, "지역", district != null ? district.getDisplayName() : null);
        appendIfPresent(sb, "방문일", visitedAt != null ? visitedAt.format(VISITED_AT_FORMAT) : null);

        if (keywords != null && !keywords.isEmpty()) {
            appendIfPresent(sb, "키워드", String.join(", ", keywords));
        }

        if (references != null && !references.isBlank()) {
            sb.append("\n참고 기록 (이 사용자가 예전에 쓴 글 — 어투만 참고):\n")
                    .append(references)
                    .append("\n");
        }

        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }
}
