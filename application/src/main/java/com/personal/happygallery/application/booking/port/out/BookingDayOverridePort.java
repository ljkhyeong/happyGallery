package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingDayOverride;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingDayOverridePort {

    Optional<BookingDayOverride> findById(LocalDate date);

    List<BookingDayOverride> findByDateBetweenOrderByDate(LocalDate dateFrom, LocalDate dateTo);

    <S extends BookingDayOverride> S save(S override);

    void deleteById(LocalDate date);
}
