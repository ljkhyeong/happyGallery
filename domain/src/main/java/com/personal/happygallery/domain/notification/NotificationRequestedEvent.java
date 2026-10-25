package com.personal.happygallery.domain.notification;

import java.util.UUID;

/**
 * 알림 발송 요청 이벤트.
 *
 * <p>수신자 유형별로 서브타입이 나뉘며, 리스너는 pattern matching 으로 분기한다.
 */
public sealed interface NotificationRequestedEvent {

    NotificationEventType eventType();

    NotificationAggregateType aggregateType();

    Long aggregateId();

    String idempotencyKey();

    record ForGuest(Long guestId, NotificationEventType eventType,
                    NotificationAggregateType aggregateType, Long aggregateId, String idempotencyKey)
            implements NotificationRequestedEvent {}

    record ForUser(Long userId, NotificationEventType eventType,
                   NotificationAggregateType aggregateType, Long aggregateId, String idempotencyKey)
            implements NotificationRequestedEvent {}

    /** 같은 대상에 여러 번 보내는 알림. 대상은 남기고 중복 방지 키만 요청마다 새로 만든다. */
    static NotificationRequestedEvent forGuestRepeatable(Long guestId, NotificationEventType eventType,
                                                         NotificationAggregateType aggregateType, Long aggregateId) {
        return new ForGuest(guestId, eventType, aggregateType, aggregateId, requestKey("GUEST", guestId, eventType));
    }

    static NotificationRequestedEvent forGuest(Long guestId, NotificationEventType eventType,
                                               NotificationAggregateType aggregateType, Long aggregateId) {
        return new ForGuest(guestId, eventType, aggregateType, aggregateId,
                aggregateKey("GUEST", guestId, eventType, aggregateType, aggregateId));
    }

    static NotificationRequestedEvent forGuestOncePerAggregate(
            Long guestId,
            NotificationEventType eventType,
            NotificationAggregateType aggregateType,
            Long aggregateId) {
        return new ForGuest(
                guestId,
                eventType,
                aggregateType,
                aggregateId,
                oncePerAggregateKeyPrefix(eventType, aggregateType) + aggregateId);
    }

    /** 같은 대상에 여러 번 보내는 알림. 대상은 남기고 중복 방지 키만 요청마다 새로 만든다. */
    static NotificationRequestedEvent forUserRepeatable(Long userId, NotificationEventType eventType,
                                                        NotificationAggregateType aggregateType, Long aggregateId) {
        return new ForUser(userId, eventType, aggregateType, aggregateId, requestKey("USER", userId, eventType));
    }

    static NotificationRequestedEvent forUser(Long userId, NotificationEventType eventType,
                                              NotificationAggregateType aggregateType, Long aggregateId) {
        return new ForUser(userId, eventType, aggregateType, aggregateId,
                aggregateKey("USER", userId, eventType, aggregateType, aggregateId));
    }

    static NotificationRequestedEvent forUserOncePerAggregate(
            Long userId,
            NotificationEventType eventType,
            NotificationAggregateType aggregateType,
            Long aggregateId) {
        return new ForUser(
                userId,
                eventType,
                aggregateType,
                aggregateId,
                oncePerAggregateKeyPrefix(eventType, aggregateType) + aggregateId);
    }

    /** 회원이면 회원 수신자, 아니면 비회원 수신자로 만든다. 멱등키는 단일 수신자 팩토리와 같다. */
    static NotificationRequestedEvent forRecipient(Long userId, Long guestId, NotificationEventType eventType,
                                                   NotificationAggregateType aggregateType, Long aggregateId) {
        return userId != null
                ? forUser(userId, eventType, aggregateType, aggregateId)
                : forGuest(guestId, eventType, aggregateType, aggregateId);
    }

    /** {@link #forRecipient}의 반복 발송 알림 대응. 대상은 남기고 중복 방지 키만 요청마다 새로 만든다. */
    static NotificationRequestedEvent forRecipientRepeatable(Long userId, Long guestId, NotificationEventType eventType,
                                                             NotificationAggregateType aggregateType, Long aggregateId) {
        return userId != null
                ? forUserRepeatable(userId, eventType, aggregateType, aggregateId)
                : forGuestRepeatable(guestId, eventType, aggregateType, aggregateId);
    }

    /** {@link #forRecipient}의 집계 단위 1회 발송 대응. */
    static NotificationRequestedEvent forRecipientOncePerAggregate(
            Long userId,
            Long guestId,
            NotificationEventType eventType,
            NotificationAggregateType aggregateType,
            Long aggregateId) {
        return userId != null
                ? forUserOncePerAggregate(userId, eventType, aggregateType, aggregateId)
                : forGuestOncePerAggregate(guestId, eventType, aggregateType, aggregateId);
    }

    private static String oncePerAggregateKeyPrefix(
            NotificationEventType eventType, NotificationAggregateType aggregateType) {
        return "AGGREGATE:" + eventType + ":" + aggregateType + ":";
    }

    private static String requestKey(String recipientType, Long recipientId, NotificationEventType eventType) {
        return recipientType + ":" + recipientId + ":" + eventType + ":REQUEST:" + UUID.randomUUID();
    }

    private static String aggregateKey(String recipientType, Long recipientId, NotificationEventType eventType,
                                       NotificationAggregateType aggregateType, Long aggregateId) {
        if (aggregateType == null || aggregateId == null) {
            return requestKey(recipientType, recipientId, eventType);
        }
        return recipientType + ":" + recipientId + ":" + eventType + ":" + aggregateType + ":" + aggregateId;
    }
}
