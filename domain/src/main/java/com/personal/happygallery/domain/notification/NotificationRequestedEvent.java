package com.personal.happygallery.domain.notification;

/**
 * 알림 발송 요청 이벤트.
 *
 * <p>guestId/userId 중 하나만 non-null이어야 한다.
 * phone/name이 non-null이면 조회 없이 바로 발송하고,
 * null이면 guestId/userId로 수신자 정보를 조회한 뒤 발송한다.
 */
public record NotificationRequestedEvent(
        Long guestId,
        Long userId,
        String phone,
        String name,
        NotificationEventType eventType
) {

    public static NotificationRequestedEvent forGuest(Long guestId, NotificationEventType eventType) {
        return new NotificationRequestedEvent(guestId, null, null, null, eventType);
    }

    public static NotificationRequestedEvent forGuestWithContact(Long guestId, String phone, String name,
                                                                  NotificationEventType eventType) {
        return new NotificationRequestedEvent(guestId, null, phone, name, eventType);
    }

    public static NotificationRequestedEvent forUser(Long userId, NotificationEventType eventType) {
        return new NotificationRequestedEvent(null, userId, null, null, eventType);
    }
}
