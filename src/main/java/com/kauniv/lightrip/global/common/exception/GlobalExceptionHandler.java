package com.kauniv.lightrip.global.common.exception;

import com.kauniv.lightrip.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", code.getCode(), e.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code, e.getMessage()));
    }

    /** validation(@Valid) 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] {}", detail);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, detail));
    }

    /** 잘못된 HTTP 메서드 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotAllowed] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /** Spring Security 인가 실패 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDenied] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.HANDLE_ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ErrorCode.HANDLE_ACCESS_DENIED));
    }

    /** DB 제약조건 위반(유니크 등) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        log.warn("[DataIntegrityViolation] {}", message);

        String lower = message == null ? "" : message.toLowerCase();

        // 1) 제약조건명으로 판별 (Postgres: duplicate key value violates unique constraint "uk_users_nickname")
        if (lower.contains("uk_users_nickname")) {
            return ResponseEntity
                    .status(ErrorCode.DUPLICATE_NICKNAME.getStatus())
                    .body(ApiResponse.error(ErrorCode.DUPLICATE_NICKNAME));
        }

        // 2) fallback: 환경에 따라 constraint명이 안 보일 수 있어 컬럼명 기반으로 한 번 더 매핑
        if (lower.contains("nickname")) {
            return ResponseEntity
                    .status(ErrorCode.DUPLICATE_NICKNAME.getStatus())
                    .body(ApiResponse.error(ErrorCode.DUPLICATE_NICKNAME));
        }

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE));
    }

    /** 그 외 모든 예외 (최후의 보루) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException]", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}