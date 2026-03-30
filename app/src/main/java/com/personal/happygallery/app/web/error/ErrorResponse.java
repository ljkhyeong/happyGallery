package com.personal.happygallery.app.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.personal.happygallery.domain.error.ErrorCode;

/**
 * 에러 응답 포맷.
 *
 * <pre>
 * {
 *   "code":      "ALREADY_REFUNDED",
 *   "message":   "이미 환불된 건입니다.",
 *   "requestId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, String requestId) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String requestId) {
        return new ErrorResponse(errorCode.name(), message, requestId);
    }
}
