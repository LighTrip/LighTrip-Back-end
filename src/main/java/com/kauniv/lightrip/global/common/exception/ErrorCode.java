package com.kauniv.lightrip.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== Common =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C004", "접근이 거부되었습니다."),

    // ===== Auth =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),

    // ===== User =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),

    // ===== Passport =====
    PASSPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 여권입니다."),
    PASSPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "P002", "해당 여권에 대한 권한이 없습니다."),
    PASSPORT_DUPLICATE(HttpStatus.CONFLICT, "P003", "같은 장소, 같은 날짜에 이미 등록된 여권이 있습니다."),
    PASSPORT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "P004", "방문 인증에 실패했습니다."),

    // ===== Scrap =====
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "스크랩 기록이 존재하지 않습니다."),
    SCRAP_DUPLICATE(HttpStatus.CONFLICT, "S002", "이미 스크랩한 여권입니다."),
    SCRAP_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "S003", "본인의 여권은 스크랩할 수 없습니다."),

    // ===== Team =====
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 팀입니다."),
    TEAM_ALREADY_JOINED(HttpStatus.CONFLICT, "T002", "이미 가입된 팀이 있습니다."),
    TEAM_INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "T003", "유효하지 않은 초대 코드입니다."),

    // ===== Map =====
    REVERSE_GEOCODE_FAILED(HttpStatus.BAD_GATEWAY, "M001", "역지오코딩에 실패했습니다."),

    // ===== Like =====
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "좋아요 기록이 존재하지 않습니다."),
    LIKE_DUPLICATE(HttpStatus.CONFLICT, "L002", "이미 좋아요한 여권입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}