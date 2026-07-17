package com.personal.happygallery.domain.notification;

import java.util.UUID;

/**
 * 알림 발송 요청 이벤트.
 *
 * <p>수신자 유형별로 서브타입이 나뉘며, 리스너는 pattern matching 으로 분기한다.
 */
public sealed interface NotificationRequestedEvent {

    NotificationEventType eventType();

    String aggregateType();

    Long aggregateId();

    String idempotencyKey();

    record ForGuest(Long guestId, NotificationEventType eventType,
                    String aggregateType, Long aggregateId, String idempotencyKey)
            implements NotificationRequestedEvent {}

    record ForUser(Long userId, NotificationEventType eventType,
                   String aggregateType, Long aggregateId, String idempotencyKey)
            implements NotificationRequestedEvent {}

    static NotificationRequestedEvent forGuest(Long guestId, NotificationEventType eventType) {
        return new ForGuest(guestId, eventType, null, null, requestKey("GUEST", guestId, eventType));
    }

    static NotificationRequestedEvent forGuest(Long guestId, NotificationEventType eventType,
                                               String aggregateType, Long aggregateId) {
        return new ForGuest(guestId, eventType, aggregateType, aggregateId,
                aggregateKey("GUEST", guestId, eventType, aggregateType, aggregateId));
    }

    static NotificationRequestedEvent forUser(Long userId, NotificationEventType eventType) {
        return new ForUser(userId, eventType, null, null, requestKey("USER", userId, eventType));
    }

    static NotificationRequestedEvent forUser(Long userId, NotificationEventType eventType,
                                              String aggregateType, Long aggregateId) {
        return new ForUser(userId, eventType, aggregateType, aggregateId,
                aggregateKey("USER", userId, eventType, aggregateType, aggregateId));
    }

    private static String requestKey(String recipientType, Long recipientId, NotificationEventType eventType) {
        return recipientType + ":" + recipientId + ":" + eventType + ":REQUEST:" + UUID.randomUUID();
    }

    private static String aggregateKey(String recipientType, Long recipientId, NotificationEventType eventType,
                                       String aggregateType, Long aggregateId) {
        if (aggregateType == null || aggregateId == null) {
            return requestKey(recipientType, recipientId, eventType);
        }
        return recipientType + ":" + recipientId + ":" + eventType + ":" + aggregateType + ":" + aggregateId;
    }
}
