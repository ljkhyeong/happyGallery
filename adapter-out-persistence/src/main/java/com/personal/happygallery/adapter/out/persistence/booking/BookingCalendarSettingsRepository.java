package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingCalendarSettingsPort;
import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingCalendarSettingsRepository
        extends JpaRepository<BookingCalendarSettings, Long>, BookingCalendarSettingsPort {

    @Override
    Optional<BookingCalendarSettings> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BookingCalendarSettings s WHERE s.id = :id")
    Optional<BookingCalendarSettings> findByIdForUpdate(@Param("id") Long id);

    @Override
    <S extends BookingCalendarSettings> S save(S settings);
}
