package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingReminderCandidatePort {

    List<BookingReminderTarget> findUnnotifiedBookedAfterId(
            LocalDateTime start,
            LocalDateTime end,
            NotificationEventType eventType,
            Long afterId,
            int limit);
}
