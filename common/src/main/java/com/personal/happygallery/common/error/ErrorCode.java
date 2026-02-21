package com.personal.happygallery.common.error;

/**
 * 애플리케이션 에러 코드.
 * httpStatus: HTTP 응답 상태코드
 * message: 기본 에러 메시지 (클라이언트 표시용)
 */
public enum ErrorCode {

    // 400 Bad Request — 입력 검증 실패
    INVALID_INPUT(400, "잘못된 입력값입니다."),

    // 404 Not Found — 리소스 미존재
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),

    // 409 Conflict — 상태 충돌
    ALREADY_REFUNDED(409, "이미 환불된 건입니다."),
    INVENTORY_NOT_ENOUGH(409, "재고가 부족합니다."),
    CAPACITY_EXCEEDED(409, "슬롯 정원이 초과되었습니다."),

    // 422 Unprocessable — 비즈니스 규칙 위반
    REFUND_NOT_ALLOWED(422, "환불 가능 기간이 지났습니다."),
    CHANGE_NOT_ALLOWED(422, "변경 가능 시간이 지났습니다."),
    PASS_EXPIRED(422, "이용권이 만료되었습니다.");

    public final int httpStatus;
    public final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
