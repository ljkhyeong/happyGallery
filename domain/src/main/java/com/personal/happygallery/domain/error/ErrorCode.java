package com.personal.happygallery.domain.error;

/**
 * 애플리케이션 에러 코드.
 * httpStatus: HTTP 응답 상태코드
 * message: 기본 에러 메시지 (클라이언트 표시용)
 */
public enum ErrorCode {

    // 400 Bad Request — 입력 검증 실패
    INVALID_INPUT(400, "잘못된 입력값입니다."),
    PHONE_VERIFICATION_FAILED(400, "휴대폰 인증에 실패했습니다. 코드를 확인하거나 재발송하세요."),
    PASSWORD_RESET_FAILED(400, "비밀번호 재설정 정보가 올바르지 않습니다."),

    // 401 Unauthorized — 인증 실패
    UNAUTHORIZED(401, "관리자 인증이 필요합니다."),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_LOGIN_FAILED(401, "소셜 로그인에 실패했습니다. 다시 시도해주세요."),

    // 403 Forbidden — 인증되었지만 권한 부족
    FORBIDDEN(403, "요청한 작업을 수행할 권한이 없습니다."),

    // 429 Too Many Requests — 처리율 제한 초과
    TOO_MANY_REQUESTS(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // 404 Not Found — 리소스 미존재
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),

    // 409 Conflict — 상태 충돌
    EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다."),
    PHONE_ALREADY_REGISTERED(409, "이미 휴대폰 번호가 등록되어 있습니다."),
    LOCAL_PASSWORD_NOT_SET(409, "이메일 로그인 비밀번호가 없습니다. 휴대폰 인증으로 비밀번호를 설정해주세요."),
    SOCIAL_ACCOUNT_LINK_REQUIRED(409, "같은 이메일로 가입된 계정이 있습니다. 기존 로그인 수단을 이용해주세요."),
    ALREADY_REFUNDED(409, "이미 환불된 건입니다."),
    INVENTORY_NOT_ENOUGH(409, "재고가 부족합니다."),
    CAPACITY_EXCEEDED(409, "슬롯 정원이 초과되었습니다."),
    DUPLICATE_BOOKING(409, "이미 예약된 슬롯입니다."),
    SLOT_NOT_AVAILABLE(409, "예약할 수 없는 슬롯입니다."),
    BOOKING_CONFLICT(409, "동시 변경 요청이 감지되었습니다. 잠시 후 다시 시도해주세요."),
    PAYMENT_CONFIRM_IN_PROGRESS(409, "결제 확정을 처리 중입니다. 잠시 후 다시 시도해주세요."),
    PAYMENT_RECONCILIATION_REQUIRED(409, "결제 승인 여부를 확인하고 있습니다. 새로 결제하지 말고 고객센터에 문의해 주세요."),
    CONFLICT(409, "처리 중 충돌이 감지되었습니다. 잠시 후 다시 시도해주세요."),

    // 410 Gone — 유효기간이 끝난 리소스
    PAYMENT_ATTEMPT_EXPIRED(410, "결제 준비 시간이 만료되었습니다. 결제를 다시 시작해주세요."),
    PAYMENT_RESULT_RETENTION_EXPIRED(410, "결제 결과 재조회 기간이 만료되었습니다."),

    // 422 Unprocessable — 비즈니스 규칙 위반
    REFUND_NOT_ALLOWED(422, "환불 가능 기간이 지났습니다."),
    PRODUCTION_REFUND_NOT_ALLOWED(422, "제작이 시작된 주문은 환불할 수 없습니다."),
    CHANGE_NOT_ALLOWED(422, "변경 가능 시간이 지났습니다."),
    PASS_EXPIRED(422, "이용권이 만료되었습니다."),
    PASS_CREDIT_INSUFFICIENT(422, "이용권 잔여 횟수가 부족합니다."),
    PHONE_VERIFICATION_REQUIRED(422, "휴대폰 인증을 완료한 뒤 다시 시도해주세요."),
    PASSWORD_UNCHANGED(422, "현재 비밀번호와 다른 새 비밀번호를 입력해주세요."),
    PAYMENT_METHOD_NOT_ALLOWED(422, "예약금은 카드 또는 간편결제만 허용됩니다. 계좌이체는 사용할 수 없습니다."),

    // 500 Internal Server Error — 서버 오류
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다."),

    // 502 Bad Gateway — 외부 PG/서비스 호출 실패
    PAYMENT_FAILED(502, "결제 확정에 실패했습니다."),

    // 503 Service Unavailable — 필수 인프라 일시 장애
    PAYMENT_CONFIRM_RETRYABLE(503, "결제 처리 결과를 확인하지 못했습니다. 잠시 후 다시 확인해 주세요."),
    SERVICE_UNAVAILABLE(503, "요청을 일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");

    public final int httpStatus;
    public final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
