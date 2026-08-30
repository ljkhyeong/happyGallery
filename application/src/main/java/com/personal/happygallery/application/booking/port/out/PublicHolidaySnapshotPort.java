package com.personal.happygallery.application.booking.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PublicHolidaySnapshotPort {

    Set<LocalDate> findDatesByYear(int year);

    void replaceYear(int year, List<PublicHoliday> holidays, LocalDateTime syncedAt);

    record PublicHoliday(LocalDate date, String name) {}
}
