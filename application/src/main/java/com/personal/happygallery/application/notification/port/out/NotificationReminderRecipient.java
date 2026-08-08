package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationRecipientType;

/** 리마인드 aggregate의 현재 회원·비회원 수신자 스냅샷. */
public record NotificationReminderRecipient(Long guestId, Long userId) {

    public NotificationReminderRecipient {
        if ((guestId == null) == (userId == null)) {
            throw new IllegalArgumentException("알림 수신자는 회원 또는 비회원 중 정확히 하나여야 합니다.");
        }
    }

    public static NotificationReminderRecipient forGuest(Long guestId) {
        return new NotificationReminderRecipient(guestId, null);
    }

    public static NotificationReminderRecipient forUser(Long userId) {
        return new NotificationReminderRecipient(null, userId);
    }

    public NotificationRecipientType recipientType() {
        return userId != null ? NotificationRecipientType.USER : NotificationRecipientType.GUEST;
    }
}
