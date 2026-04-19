package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface BookingReminderBatchUseCase {
    BatchResult sendD1Reminders();
    BatchResult sendSameDayReminders();
}
