package com.personal.happygallery.application.notification.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

/** 시간 의존 리마인드가 발송 시점에도 현재 도메인 상태와 시간 구간에 맞는지 조회한다. */
public interface NotificationReminderEligibilityPort {

    Optional<NotificationReminderRecipient> findD1BookingRecipient(
            Long bookingId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    Optional<NotificationReminderRecipient> findSameDayBookingRecipient(
            Long bookingId, LocalDateTime startExclusive, LocalDateTime endExclusive);

    Optional<NotificationReminderRecipient> findPassExpiryRecipient(
            Long passId, LocalDateTime nowExclusive, LocalDateTime latestExpiryInclusive);

    Optional<NotificationReminderRecipient> findPickupDeadlineRecipient(
            Long orderId, LocalDateTime nowExclusive, LocalDateTime latestDeadlineInclusive);
}
