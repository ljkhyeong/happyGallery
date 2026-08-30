package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingDayOverridePort;
import com.personal.happygallery.domain.booking.BookingDayOverride;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingDayOverrideRepository
        extends JpaRepository<BookingDayOverride, LocalDate>, BookingDayOverridePort {

    @Override
    Optional<BookingDayOverride> findById(LocalDate date);

    @Override
    List<BookingDayOverride> findByDateBetweenOrderByDate(LocalDate dateFrom, LocalDate dateTo);

    @Override
    <S extends BookingDayOverride> S save(S override);

    @Override
    void deleteById(LocalDate date);
}
