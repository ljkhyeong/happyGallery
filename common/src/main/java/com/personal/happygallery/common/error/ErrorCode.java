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
    DUPLICATE_BOOKING(409, "이미 예약된 슬롯입니다."),
    SLOT_NOT_AVAILABLE(409, "예약할 수 없는 슬롯입니다."),
    BOOKING_CONFLICT(409, "동시 변경 요청이 감지되었습니다. 잠시 후 다시 시도해주세요."),

    // 400 Bad Request — 인증 실패
    PHONE_VERIFICATION_FAILED(400, "휴대폰 인증에 실패했습니다. 코드를 확인하거나 재발송하세요."),

    // 422 Unprocessable — 비즈니스 규칙 위반
    REFUND_NOT_ALLOWED(422, "환불 가능 기간이 지났습니다."),
    PRODUCTION_REFUND_NOT_ALLOWED(422, "제작이 시작된 주문은 환불할 수 없습니다."),
    CHANGE_NOT_ALLOWED(422, "변경 가능 시간이 지났습니다."),
    PASS_EXPIRED(422, "이용권이 만료되었습니다."),
    PASS_CREDIT_INSUFFICIENT(422, "이용권 잔여 횟수가 부족합니다."),
    PAYMENT_METHOD_NOT_ALLOWED(422, "예약금은 카드 또는 간편결제만 허용됩니다. 계좌이체는 사용할 수 없습니다.");

    public final int httpStatus;
    public final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
