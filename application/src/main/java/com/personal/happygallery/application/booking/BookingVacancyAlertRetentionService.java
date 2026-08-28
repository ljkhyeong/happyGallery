package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertRetentionPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingVacancyAlertRetentionService {

    private final BookingVacancyAlertRetentionPort retentionPort;

    public BookingVacancyAlertRetentionService(BookingVacancyAlertRetentionPort retentionPort) {
        this.retentionPort = retentionPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteExpiredBatch(
            LocalDateTime now, LocalDateTime terminalCutoff, int batchSize) {
        return retentionPort.deleteExpiredBatch(now, terminalCutoff, batchSize);
    }
}
