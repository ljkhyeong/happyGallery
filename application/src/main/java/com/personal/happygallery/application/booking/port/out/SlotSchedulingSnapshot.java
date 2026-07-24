package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClassStatus;
import java.time.LocalDateTime;

public record SlotSchedulingSnapshot(
        Long id,
        Long classId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BookingClassStatus classStatus,
        boolean adminActive,
        int bufferBlockCount
) {

    public boolean isReservableAt(LocalDateTime now) {
        return classStatus == BookingClassStatus.ACTIVE
                && adminActive
                && bufferBlockCount == 0
                && startAt.isAfter(now);
    }
}
