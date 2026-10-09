package com.personal.happygallery.domain.notification;

/**
 * 알림이 가리키는 업무 대상 종류. aggregateId는 각 상수가 적힌 대상의 ID다.
 *
 * <p>outbox {@code aggregate_type} 컬럼과 멱등 키에 상수 이름을 그대로 쓰므로 이름을 바꾸지 않는다.
 */
public enum NotificationAggregateType {
    /** 주문 */
    ORDER,
    /** 주문 반품·교환 요청 */
    ORDER_CLAIM,
    /** 예약 */
    BOOKING,
    /** 예약 빈자리 알림 신청 */
    VACANCY_ALERT,
    /** 이용권 구매 */
    PASS_PURCHASE,
    /** 예약금·주문·이용권 환불 */
    REFUND,
    /** 1:1 문의 */
    INQUIRY,
    /** 상품 Q&A */
    PRODUCT_QNA,
    /** 상품 재입고 알림 신청 */
    RESTOCK_ALERT,
    /** 후기 */
    REVIEW,
    /** 후기 운영 조치(숨김·재공개) */
    REVIEW_MODERATION_ACTION
}
