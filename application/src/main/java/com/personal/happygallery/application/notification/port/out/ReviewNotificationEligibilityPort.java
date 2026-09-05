package com.personal.happygallery.application.notification.port.out;

import java.util.Optional;

/** 현재 주문·예약·후기 상태를 확인해 알림을 받을 수신자를 조회한다. */
public interface ReviewNotificationEligibilityPort {

    Optional<NotificationReminderRecipient> findOrderRequestRecipient(Long orderId);

    Optional<NotificationReminderRecipient> findBookingRequestRecipient(Long bookingId);

    Optional<NotificationReminderRecipient> findHiddenReviewRecipient(Long moderationActionId);

    Optional<NotificationReminderRecipient> findRepublishedReviewRecipient(Long moderationActionId);

    Optional<NotificationReminderRecipient> findOwnerRepliedReviewRecipient(Long reviewId);
}
