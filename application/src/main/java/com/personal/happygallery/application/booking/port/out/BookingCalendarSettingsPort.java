package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import java.util.Optional;

public interface BookingCalendarSettingsPort {

    Optional<BookingCalendarSettings> findById(Long id);

    Optional<BookingCalendarSettings> findByIdForUpdate(Long id);

    <S extends BookingCalendarSettings> S save(S settings);
}
