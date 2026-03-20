package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.app.batch.BatchResult;

public interface BookingReminderBatchUseCase {
    BatchResult sendD1Reminders();
    BatchResult sendSameDayReminders();
}
