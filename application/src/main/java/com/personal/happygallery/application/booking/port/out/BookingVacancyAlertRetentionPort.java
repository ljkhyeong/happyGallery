package com.personal.happygallery.application.booking.port.out;

import java.time.LocalDateTime;

public interface BookingVacancyAlertRetentionPort {

    int deleteExpiredBatch(LocalDateTime now, LocalDateTime terminalCutoff, int batchSize);
}
