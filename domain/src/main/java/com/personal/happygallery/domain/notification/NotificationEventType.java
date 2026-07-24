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
    /** D-1 리마인드 (배치) */
    REMINDER_D1,
    /** 당일 아침 리마인드 (배치) */
    REMINDER_SAME_DAY,
    /** 8회권 만료 7일 전 알림 (배치) */
    PASS_EXPIRY_SOON,
    /** 픽업 마감 2시간 전 알림 (배치) */
    PICKUP_DEADLINE_REMINDER
}
