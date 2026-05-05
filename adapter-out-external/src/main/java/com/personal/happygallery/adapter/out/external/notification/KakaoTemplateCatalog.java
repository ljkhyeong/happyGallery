package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;

/**
 * 카카오 알림톡 이벤트 → 템플릿 코드 매핑.
 *
 * <p>{@link KakaoAlimtalkSender}와 분리해 두면 신규 이벤트 추가 시
 * sender 본문을 건드리지 않고 매핑만 갱신할 수 있다.
 */
public class KakaoTemplateCatalog {

    public String resolveTemplateCode(NotificationEventType eventType) {
        return switch (eventType) {
            case BOOKING_CONFIRMED -> "HG_BOOKING_CONFIRMED";
            case BOOKING_RESCHEDULED -> "HG_BOOKING_RESCHEDULED";
            case BOOKING_CANCELED -> "HG_BOOKING_CANCELED";
            case DEPOSIT_REFUNDED -> "HG_DEPOSIT_REFUNDED";
            case ORDER_PAID -> "HG_ORDER_PAID";
            case ORDER_REFUNDED -> "HG_ORDER_REFUNDED";
            case REMINDER_D1 -> "HG_REMINDER_D1";
            case REMINDER_SAME_DAY -> "HG_REMINDER_SAME_DAY";
            case PASS_EXPIRY_SOON -> "HG_PASS_EXPIRY_SOON";
            case PICKUP_DEADLINE_REMINDER -> "HG_PICKUP_DEADLINE";
        };
    }
}
