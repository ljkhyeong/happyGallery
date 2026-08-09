package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;

/**
 * 카카오 알림톡 이벤트 → 템플릿 코드 매핑.
 *
 * <p>{@link NhnAlimtalkSender}와 분리해 두면 신규 이벤트 추가 시
 * sender 본문을 건드리지 않고 매핑만 갱신할 수 있다.
 */
public final class KakaoTemplateCatalog {

    private KakaoTemplateCatalog() {
    }

    public static String resolveTemplateCode(NotificationEventType eventType) {
        return switch (eventType) {
            case BOOKING_CONFIRMED -> "HG_BOOKING_CONFIRMED";
            case BOOKING_RESCHEDULED -> "HG_BOOKING_CHANGED";
            case BOOKING_CANCELED -> "HG_BOOKING_CANCELED";
            case DEPOSIT_REFUNDED -> "HG_DEPOSIT_REFUNDED";
            case ORDER_PAID -> "HG_ORDER_PAID";
            case ORDER_APPROVED -> "HG_ORDER_APPROVED";
            case ORDER_PICKUP_READY -> "HG_PICKUP_READY";
            case ORDER_SHIPPED -> "HG_ORDER_SHIPPED";
            case ORDER_DELAY_REQUESTED -> "HG_ORDER_DELAY";
            case ORDER_REFUNDED -> "HG_ORDER_REFUNDED";
            case ORDER_CLAIM_RESOLVED -> "HG_ORDER_CLAIM";
            case ORDER_EXCHANGE_COMPLETED -> "HG_ORDER_EXCHANGE";
            case PASS_PURCHASED -> "HG_PASS_PURCHASED";
            case PASS_REFUNDED -> "HG_PASS_REFUNDED";
            case INQUIRY_ANSWERED -> "HG_INQUIRY_ANSWERED";
            case PRODUCT_QNA_ANSWERED -> "HG_QNA_ANSWERED";
            case REVIEW_REQUEST -> "HG_REVIEW_REQUEST";
            case REVIEW_HIDDEN -> "HG_REVIEW_HIDDEN";
            case REVIEW_REPUBLISHED -> "HG_REVIEW_REOPENED";
            case REVIEW_OWNER_REPLIED -> "HG_REVIEW_REPLY";
            case REMINDER_D1 -> "HG_REMINDER_D1";
            case REMINDER_SAME_DAY -> "HG_REMINDER_SAME_DAY";
            case PASS_EXPIRY_SOON -> "HG_PASS_EXPIRY_SOON";
            case PICKUP_DEADLINE_REMINDER -> "HG_PICKUP_DEADLINE";
        };
    }
}
