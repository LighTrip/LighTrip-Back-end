package com.kauniv.lightrip.global.ai.prompt;

import com.kauniv.lightrip.global.enums.Category;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 여권 초안 생성 프롬프트. 문구 수정은 이 파일만 고치면 된다.
 */
public final class DraftPrompt {

    private DraftPrompt() {
    }

    // > Category enum에서 직접 만든다. 카테고리가 추가·변경돼도 프롬프트가 따라오도록.
    private static final String CATEGORY_GUIDE = Arrays.stream(Category.values())
            .map(c -> c.name() + "(" + c.getDisplayName() + ")")
            .collect(Collectors.joining(", "));

    public static final String SYSTEM = """
            당신은 여행 기록 앱 '라이트립'의 기록 초안 작성 도우미입니다.
            사용자가 올린 사진과 메모를 받아, 사용자가 직접 쓴 것 같은 짧은 여행 기록 초안을 만들고
            그 장소의 카테고리를 분류합니다.

            초안 작성 규칙
            - 한국어로 작성합니다.
            - 반드시 2문장 이상 4문장 이하로 씁니다.
            - 담백하고 사실 위주인 여행 기록 톤을 유지합니다. 과장된 감탄사, 광고 문구, 해시태그, 이모지는 쓰지 않습니다.
            - 사진에서 분명히 보이는 것과 메모에 적힌 내용만 씁니다.
              가격, 메뉴, 동행자, 날씨처럼 확인되지 않는 사실은 지어내지 않습니다.
            - 사진을 설명하는 문장('사진 속에는...', '이 사진은...')은 쓰지 않습니다. 방문한 사람의 시점으로 씁니다.
            - '참고 기록'이 주어지면 어투와 문장 길이만 참고합니다. 내용을 그대로 가져오지 않습니다.

            카테고리 분류 규칙
            - 아래 중 하나를 고릅니다: %s
            - 영문 키값으로 출력합니다. 애매하면 ETC를 고릅니다.

            출력 형식
            - 아래 형태의 JSON 객체 하나만 출력합니다.
            - 설명, 코드블록, 그 밖의 텍스트를 덧붙이지 않습니다.
            {"draft": "초안 본문", "category": "CAFE"}
            """.formatted(CATEGORY_GUIDE);

    /**
     * 사용자 메시지 구성. 사진은 별도 이미지 파트로 전달되므로 여기에는 텍스트만 담는다.
     *
     * @param text       사용자가 입력한 메모. 없으면 null.
     * @param references 과거 기록 기반 참고 문장. 없으면 null.
     */
    public static String user(String text, String references) {
        StringBuilder sb = new StringBuilder();

        if (text != null && !text.isBlank()) {
            sb.append("아래 메모와 사진으로 여행 기록 초안을 작성하고 카테고리를 분류해주세요.\n\n")
                    .append("메모:\n")
                    .append(text.trim())
                    .append("\n");
        } else {
            // > 메모 없이 사진만 온 경우. 사진에서 읽어낼 수 있는 것만으로 쓰게 한다.
            sb.append("사진을 보고 여행 기록 초안을 작성하고 카테고리를 분류해주세요.\n");
        }

        if (references != null && !references.isBlank()) {
            sb.append("\n참고 기록 (이 사용자가 예전에 쓴 글 — 어투만 참고):\n")
                    .append(references)
                    .append("\n");
        }

        return sb.toString();
    }
}
