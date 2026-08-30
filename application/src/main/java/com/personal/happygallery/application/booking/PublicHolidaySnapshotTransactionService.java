package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort.PublicHoliday;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PublicHolidaySnapshotTransactionService {

    private final PublicHolidaySnapshotPort snapshotPort;

    PublicHolidaySnapshotTransactionService(PublicHolidaySnapshotPort snapshotPort) {
        this.snapshotPort = snapshotPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void replace(int year, List<PublicHoliday> holidays, LocalDateTime syncedAt) {
        snapshotPort.replaceYear(year, holidays, syncedAt);
    }
}
