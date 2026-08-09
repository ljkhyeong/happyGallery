package com.personal.happygallery.domain.notification;

/** 알림 발송 이벤트 유형. notification_log.event_type 컬럼 값으로 사용된다. */
public enum NotificationEventType {
    /** 예약 완료 */
    BOOKING_CONFIRMED,
    /** 예약 변경 */
    BOOKING_RESCHEDULED,
    /** 예약 취소 */
    BOOKING_CANCELED,
    /** 예약금 환불 */
    DEPOSIT_REFUNDED,
    /** 주문 결제 완료 */
    ORDER_PAID,
    /** 주문 승인 */
    ORDER_APPROVED,
    /** 픽업 준비 완료 */
    ORDER_PICKUP_READY,
    /** 배송 시작 */
    ORDER_SHIPPED,
    /** 주문 처리 지연 안내 */
    ORDER_DELAY_REQUESTED,
    /** 주문 환불 */
    ORDER_REFUNDED,
    /** 주문 클레임 승인 또는 거절 */
    ORDER_CLAIM_RESOLVED,
    /** 주문 교환 처리 완료 */
    ORDER_EXCHANGE_COMPLETED,
    /** 8회권 구매 완료 */
    PASS_PURCHASED,
    /** 8회권 환불 완료 */
    PASS_REFUNDED,
    /** 1:1 문의 답변 등록 */
    INQUIRY_ANSWERED,
    /** 상품 Q&A 답변 등록 */
    PRODUCT_QNA_ANSWERED,
    /** 완료 거래 후기 작성 요청 */
    REVIEW_REQUEST,
    /** 작성한 후기 숨김 안내 */
    REVIEW_HIDDEN,
    /** 작성한 후기 재공개 안내 */
    REVIEW_REPUBLISHED,
    /** 작성한 후기에 공방 공식 답글 등록 */
    REVIEW_OWNER_REPLIED,
    /** D-1 리마인드 (배치) */
    REMINDER_D1,
    /** 당일 아침 리마인드 (배치) */
    REMINDER_SAME_DAY,
    /** 8회권 만료 7일 전 알림 (배치) */
    PASS_EXPIRY_SOON,
    /** 픽업 마감 2시간 전 알림 (배치) */
    PICKUP_DEADLINE_REMINDER;

    public boolean isTimeSensitiveReminder() {
        return switch (this) {
            case REMINDER_D1, REMINDER_SAME_DAY, PASS_EXPIRY_SOON, PICKUP_DEADLINE_REMINDER -> true;
            case BOOKING_CONFIRMED,
                    BOOKING_RESCHEDULED,
                    BOOKING_CANCELED,
                    DEPOSIT_REFUNDED,
                    ORDER_PAID,
                    ORDER_APPROVED,
                    ORDER_PICKUP_READY,
                    ORDER_SHIPPED,
                    ORDER_DELAY_REQUESTED,
                    ORDER_REFUNDED,
                    ORDER_CLAIM_RESOLVED,
                    ORDER_EXCHANGE_COMPLETED,
                    PASS_PURCHASED,
                    PASS_REFUNDED,
                    INQUIRY_ANSWERED,
                    PRODUCT_QNA_ANSWERED,
                    REVIEW_REQUEST,
                    REVIEW_HIDDEN,
                    REVIEW_REPUBLISHED,
                    REVIEW_OWNER_REPLIED -> false;
        };
    }

    /** 발송 직전 후기·원천의 현재 상태를 다시 확인해야 하는 이벤트인지 반환한다. */
    public boolean requiresReviewRelevanceCheck() {
        return switch (this) {
            case REVIEW_REQUEST, REVIEW_HIDDEN, REVIEW_REPUBLISHED, REVIEW_OWNER_REPLIED -> true;
            case BOOKING_CONFIRMED,
                    BOOKING_RESCHEDULED,
                    BOOKING_CANCELED,
                    DEPOSIT_REFUNDED,
                    ORDER_PAID,
                    ORDER_APPROVED,
                    ORDER_PICKUP_READY,
                    ORDER_SHIPPED,
                    ORDER_DELAY_REQUESTED,
                    ORDER_REFUNDED,
                    ORDER_CLAIM_RESOLVED,
                    ORDER_EXCHANGE_COMPLETED,
                    PASS_PURCHASED,
                    PASS_REFUNDED,
                    INQUIRY_ANSWERED,
                    PRODUCT_QNA_ANSWERED,
                    REMINDER_D1,
                    REMINDER_SAME_DAY,
                    PASS_EXPIRY_SOON,
                    PICKUP_DEADLINE_REMINDER -> false;
        };
    }
}
