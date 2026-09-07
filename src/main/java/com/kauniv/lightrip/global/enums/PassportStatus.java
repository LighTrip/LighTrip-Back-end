package com.kauniv.lightrip.global.enums;

/**
 * 여권(게시물)의 노출 상태.
 *
 * <p>공개 범위를 나타내는 {@link Visibility}와 분리된 축이다.
 * {@code visibility}는 작성자의 의도(전체/친구/비공개)이고,
 * {@code status}는 신고 누적·운영 조치에 의한 시스템 상태다.</p>
 *
 * <ul>
 *   <li>{@link #ACTIVE}  — 정상. 피드·상세·불빛 등 모든 조회에 노출.</li>
 *   <li>{@link #HIDDEN}  — 서로 다른 신고자가 임계값 이상 신고하여 자동 숨김.
 *                          공개 조회에서 제외되며 작성자 본인 목록에서만 열람 가능.</li>
 *   <li>{@link #DELETED} — 운영자 영구 삭제(예약값, 현재 미사용).</li>
 * </ul>
 */
public enum PassportStatus {
    ACTIVE,
    HIDDEN,
    DELETED
}
