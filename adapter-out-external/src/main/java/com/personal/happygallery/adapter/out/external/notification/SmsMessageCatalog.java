package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;

/**
 * SMS 본문 렌더링.
 *
 * <p>{@link RealSmsSender}와 분리해 두면 신규 이벤트 추가 시
 * sender 본문을 건드리지 않고 메시지 문구만 갱신할 수 있다.
 */
public final class SmsMessageCatalog {

    private static final String PREFIX = "[해피갤러리] ";

    private SmsMessageCatalog() {
    }

    public static String render(String recipientName, NotificationEventType eventType) {
        return switch (eventType) {
            case BOOKING_CONFIRMED -> PREFIX + recipientName + "님, 예약이 확정되었습니다.";
            case BOOKING_RESCHEDULED -> PREFIX + recipientName + "님, 예약이 변경되었습니다.";
            case BOOKING_CANCELED -> PREFIX + recipientName + "님, 예약이 취소되었습니다.";
            case DEPOSIT_REFUNDED -> PREFIX + recipientName + "님, 예약금이 환불되었습니다.";
            case ORDER_PAID -> PREFIX + recipientName + "님, 주문 결제가 완료되었습니다.";
            case ORDER_APPROVED -> PREFIX + recipientName + "님, 주문이 승인되었습니다.";
            case ORDER_PICKUP_READY -> PREFIX + recipientName
                    + "님, 픽업 준비가 완료되었습니다. 주문 상세에서 픽업 마감을 확인해 주세요.";
            case ORDER_SHIPPED -> PREFIX + recipientName
                    + "님, 배송이 시작되었습니다. 주문 상세에서 운송장을 확인해 주세요.";
            case ORDER_DELAY_REQUESTED -> PREFIX + recipientName
                    + "님, 주문 처리 일정 지연 동의가 필요합니다. 주문 상세에서 응답해 주세요.";
            case ORDER_REFUNDED -> PREFIX + recipientName + "님, 주문이 환불되었습니다.";
            case ORDER_CLAIM_RESOLVED -> PREFIX + recipientName
                    + "님, 주문 클레임 처리 결과가 등록되었습니다. 주문 상세에서 확인해 주세요.";
            case ORDER_EXCHANGE_COMPLETED -> PREFIX + recipientName
                    + "님, 주문 교환 처리가 완료되었습니다. 주문 상세에서 확인해 주세요.";
            case PASS_PURCHASED -> PREFIX + recipientName + "님, 8회권 결제가 완료되었습니다.";
            case PASS_REFUNDED -> PREFIX + recipientName + "님, 8회권 환불이 완료되었습니다.";
            case INQUIRY_ANSWERED -> PREFIX + recipientName + "님, 1:1 문의에 답변이 등록되었습니다.";
            case PRODUCT_QNA_ANSWERED -> PREFIX + recipientName + "님, 상품 Q&A에 답변이 등록되었습니다.";
            case REMINDER_D1 -> PREFIX + recipientName + "님, 내일 체험이 예정되어 있습니다.";
            case REMINDER_SAME_DAY -> PREFIX + recipientName + "님, 오늘 체험이 예정되어 있습니다.";
            case PASS_EXPIRY_SOON -> PREFIX + recipientName + "님, 8회권 만료가 7일 남았습니다.";
            case PICKUP_DEADLINE_REMINDER -> PREFIX + recipientName + "님, 픽업 마감이 2시간 남았습니다.";
        };
    }
}
