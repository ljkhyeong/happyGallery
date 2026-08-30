package com.personal.happygallery.application.notification.port.out;

import java.util.Optional;

/** 후기 알림이 발송 시점에도 의미가 있는지 현재 원천·후기 상태와 함께 조회한다. */
public interface ReviewNotificationEligibilityPort {

    Optional<NotificationReminderRecipient> findOrderRequestRecipient(Long orderId);

    Optional<NotificationReminderRecipient> findBookingRequestRecipient(Long bookingId);

    Optional<NotificationReminderRecipient> findHiddenReviewRecipient(Long moderationActionId);

    Optional<NotificationReminderRecipient> findRepublishedReviewRecipient(Long moderationActionId);

    Optional<NotificationReminderRecipient> findOwnerRepliedReviewRecipient(Long reviewId);
}
