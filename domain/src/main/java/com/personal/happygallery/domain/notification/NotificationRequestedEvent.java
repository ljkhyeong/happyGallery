package com.personal.happygallery.domain.notification;

/**
 * 알림 발송 요청 이벤트.
 *
 * <p>수신자 유형별로 서브타입이 나뉘며, 리스너는 pattern matching 으로 분기한다.
 */
public sealed interface NotificationRequestedEvent {

    NotificationEventType eventType();

    record ForGuest(Long guestId, NotificationEventType eventType)
            implements NotificationRequestedEvent {}

    record ForGuestWithContact(Long guestId, String phone, String name,
                               NotificationEventType eventType)
            implements NotificationRequestedEvent {}

    record ForUser(Long userId, NotificationEventType eventType)
            implements NotificationRequestedEvent {}

    static NotificationRequestedEvent forGuest(Long guestId, NotificationEventType eventType) {
        return new ForGuest(guestId, eventType);
    }

    static NotificationRequestedEvent forGuestWithContact(Long guestId, String phone, String name,
                                                          NotificationEventType eventType) {
        return new ForGuestWithContact(guestId, phone, name, eventType);
    }

    static NotificationRequestedEvent forUser(Long userId, NotificationEventType eventType) {
        return new ForUser(userId, eventType);
    }
}
