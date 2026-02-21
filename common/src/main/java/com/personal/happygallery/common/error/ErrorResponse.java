package com.personal.happygallery.common.error;

/**
 * 에러 응답 포맷.
 *
 * <pre>
 * {
 *   "code":    "ALREADY_REFUNDED",
 *   "message": "이미 환불된 건입니다."
 * }
 * </pre>
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
