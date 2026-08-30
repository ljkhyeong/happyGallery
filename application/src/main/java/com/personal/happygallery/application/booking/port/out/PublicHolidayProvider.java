package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort.PublicHoliday;
import java.util.List;
import java.util.Optional;

public interface PublicHolidayProvider {

    boolean isEnabled();

    Optional<List<PublicHoliday>> fetch(int year);
}
