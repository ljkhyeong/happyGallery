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
    /** 주문 환불 */
    ORDER_REFUNDED,
    /** D-1 리마인드 (배치) */
    REMINDER_D1,
    /** 당일 아침 리마인드 (배치) */
    REMINDER_SAME_DAY
}
